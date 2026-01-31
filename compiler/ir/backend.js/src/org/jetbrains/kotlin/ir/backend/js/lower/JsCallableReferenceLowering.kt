/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name
import kotlin.collections.plus

@PhasePrerequisites(PropertyReferenceLowering::class, FunctionInlining::class)
class JsCallableReferenceLowering(private val jsContext: JsIrBackendContext) : WebCallableReferenceLowering(jsContext) {
    override fun getClassOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin = when {
        reference.isKReference || !reference.isLambda -> FUNCTION_REFERENCE_IMPL
        else -> LAMBDA_IMPL
    }

    override fun getConstructorCallOrigin(reference: IrRichFunctionReference) = JsStatementOrigins.CALLABLE_REFERENCE_CREATE

    private val IrRichFunctionReference.shouldAddContinuation: Boolean
        get() = isLambda && invokeFunction.isSuspend && !context.compileSuspendAsJsGenerator

    override fun IrBuilderWithScope.generateSuperClassConstructorCall(
        constructor: IrConstructor,
        superClassType: IrType,
        functionReference: IrRichFunctionReference,
    ): IrDelegatingConstructorCall {
        val superConstructor = superClassType.classOrFail.owner.primaryConstructor
            ?: compilationException("Missing primary constructor", superClassType.classOrFail.owner)
        return irDelegatingConstructorCall(superConstructor).apply {
            if (functionReference.shouldAddContinuation) {
                val continuation = constructor.parameters.single { it.origin == IrDeclarationOrigin.CONTINUATION }
                arguments[0] = IrGetValueImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = continuation.type,
                    symbol = continuation.symbol,
                    origin = JsStatementOrigins.CALLABLE_REFERENCE_INVOKE,
                )
            } else if (functionReference.reflectionTargetSymbol != null) {
                arguments[0] = functionReference.getFlags().toIrConst(context.irBuiltIns.intType)
                arguments[1] = functionReference.getArity().toIrConst(context.irBuiltIns.intType)
                arguments[2] = functionReference.getFqName(jsContext).toIrConst(context.irBuiltIns.stringType)
            }
        }
    }

    override fun getSuperClassType(reference: IrRichFunctionReference): IrType = when {
        reference.shouldAddContinuation -> context.symbols.coroutineImpl.owner.defaultType
        reference.reflectionTargetSymbol != null -> jsContext.symbols.reflectionSymbols.kFunctionImpl.defaultType
        else -> jsContext.irBuiltIns.anyType
    }

    override fun generateExtraMethods(functionReferenceClass: IrClass, reference: IrRichFunctionReference) {
        super.generateExtraMethods(functionReferenceClass, reference)
        if (reference.reflectionTargetLinkageError != null) return
        if (reference.reflectionTargetSymbol == null) return
        if (reference.boundValues.isEmpty()) return
        if (reference.shouldAddContinuation) return

        fun addOverrideInner(name: String, value: IrBuilderWithScope.(IrFunction) -> IrExpression) {
            val overridden = functionReferenceClass.superTypes.mapNotNull { superType ->
                superType.getClass()
                    ?.declarations
                    ?.filterIsInstance<IrSimpleFunction>()
                    ?.singleOrNull { it.name.asString() == name }
                    ?.symbol
            }
            require(overridden.isNotEmpty())
            val function = functionReferenceClass.addFunction {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                this.name = Name.identifier(name)
                modality = Modality.FINAL
                returnType = overridden[0].owner.returnType
            }
            function.parameters += function.createDispatchReceiverParameterWithClassParent()
            function.overriddenSymbols += overridden
            function.body = context.createIrBuilder(function.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +irReturn(value(function))
            }
        }

        val fields = functionReferenceClass.fields.toList()
        when (fields.size) {
            0 -> {}
            1 -> addOverrideInner("computeReceiver") { f ->
                irGetField(irGet(f.dispatchReceiverParameter!!), fields[0])
            }
            else -> TODO("Code generation for references with several bound receivers is not supported yet")
        }
    }

    override fun getExtraConstructorParameters(constructor: IrConstructor, reference: IrRichFunctionReference): List<IrValueParameter> {
        if (!reference.shouldAddContinuation) return emptyList()
        return listOf(
            buildValueParameter(constructor) {
                val superContinuation = context.symbols.coroutineImpl.owner.primaryConstructor!!.parameters.single()
                name = superContinuation.name
                type = superContinuation.type
                origin = IrDeclarationOrigin.CONTINUATION
                kind = IrParameterKind.Regular
            }
        )
    }
}

private fun IrRichFunctionReference.getFlags(): Int = listOfNotNull(
    (1 shl 0).takeIf { invokeFunction.isSuspend },
    (1 shl 1).takeIf { hasVarargConversion },
    (1 shl 2).takeIf { hasSuspendConversion },
    (1 shl 3).takeIf { hasUnitConversion },
    (1 shl 4).takeIf { isFunInterfaceConstructorAdapter() },
).sum()

private fun IrRichFunctionReference.getArity(): Int =
    invokeFunction.parameters.size - boundValues.size + if (invokeFunction.isSuspend) 1 else 0

private fun IrRichFunctionReference.getFqName(backendContext: JsIrBackendContext): String = when {
    isFunInterfaceConstructorAdapter() -> invokeFunction.returnType.getClass()!!.fqNameForIrSerialization.toString()
    else -> (backendContext.irFactory as IrFactoryImplForJsIC).declarationSignature(reflectionTargetSymbol!!.owner).toString()
}

private fun IrRichFunctionReference.isFunInterfaceConstructorAdapter() =
    invokeFunction.origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR