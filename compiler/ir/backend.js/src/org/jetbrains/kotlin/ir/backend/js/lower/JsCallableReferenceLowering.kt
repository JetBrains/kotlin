/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.AbstractFunctionReferenceLowering
import org.jetbrains.kotlin.backend.common.reflectedNameAccessor
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

class JsCallableReferenceLowering(context: JsCommonBackendContext) : CallableReferenceLowering(context) {
    private val IrRichFunctionReference.shouldAddContinuation: Boolean
        get() = isLambda && invokeFunction.isSuspend && !context.compileSuspendAsJsGenerator

    override fun getClassOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin {
        return if (reference.isKReference || !reference.isLambda) FUNCTION_REFERENCE_IMPL else LAMBDA_IMPL
    }

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
            }
        }
    }

    override fun getSuperClassType(reference: IrRichFunctionReference): IrType {
        return if (reference.shouldAddContinuation) {
            context.symbols.coroutineImpl.owner.defaultType
        } else {
            context.irBuiltIns.anyType
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

abstract class CallableReferenceLowering(context: JsCommonBackendContext) : AbstractFunctionReferenceLowering<JsCommonBackendContext>(context) {
    protected val IrRichFunctionReference.isLambda: Boolean
        get() = origin.isLambda

    protected val IrRichFunctionReference.isKReference: Boolean
        get() = type.let { it.isKFunction() || it.isKSuspendFunction() }

    private val nothingType = context.irBuiltIns.nothingType
    private val stringType = context.irBuiltIns.stringType

    private val IrRichFunctionReference.secondFunctionInterface: IrClass?
        get() =
            // If we implement KFunctionN we also need FunctionN
            if (isKReference) {
                val referenceType = type as IrSimpleType
                val arity = referenceType.arguments.size - 1
                if (invokeFunction.isSuspend)
                    context.symbols.suspendFunctionN(arity).owner
                else
                    context.symbols.functionN(arity).owner
            } else null

    private fun StringBuilder.collectNamesForLambda(d: IrDeclarationWithName) {
        val parent = d.parent

        if (parent is IrPackageFragment) {
            append(d.name.asString())
            return
        }

        collectNamesForLambda(parent as IrDeclarationWithName)

        if (d is IrAnonymousInitializer) return

        fun IrDeclaration.isLambdaFun(): Boolean = origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA

        when {
            d.isLambdaFun() -> {
                append('$')
                if (d is IrSimpleFunction && d.isSuspend) append('s')
                append("lambda")
            }
            d.name == SpecialNames.NO_NAME_PROVIDED -> append("\$o")
            else -> {
                append('$')
                append(d.name.asString())
            }
        }
    }

    override fun getReferenceClassName(reference: IrRichFunctionReference): Name {
        val sb = StringBuilder()
        sb.collectNamesForLambda(reference.reflectionTargetSymbol?.owner ?: reference.invokeFunction)
        if (!reference.isLambda) sb.append("\$ref")
        return Name.identifier(sb.toString())
    }

    override fun getAdditionalInterfaces(reference: IrRichFunctionReference): List<IrType> =
        listOfNotNull(reference.secondFunctionInterface?.symbol?.typeWithArguments((reference.type.removeProjections() as IrSimpleType).arguments))

    override fun postprocessInvoke(invokeFunction: IrSimpleFunction, functionReference: IrRichFunctionReference) {
        val superInvokeFun = functionReference.secondFunctionInterface?.invokeFun ?: return
        invokeFunction.overriddenSymbols = invokeFunction.overriddenSymbols memoryOptimizedPlus superInvokeFun.symbol
    }

    override fun IrBuilderWithScope.getExtraConstructorArgument(
        parameter: IrValueParameter,
        reference: IrRichFunctionReference,
    ) = irNull()

    override fun getConstructorOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin {
        return GENERATED_MEMBER_IN_CALLABLE_REFERENCE
    }

    override fun getInvokeMethodOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin {
        return IrDeclarationOrigin.DEFINED
    }

    override fun getConstructorCallOrigin(reference: IrRichFunctionReference) = JsStatementOrigins.CALLABLE_REFERENCE_CREATE

    private fun createNameProperty(clazz: IrClass, reference: IrRichFunctionReference) {
        // TODO(KT-76093): Support partial linkage for names of function references
        val reflectionTargetSymbol = reference.reflectionTargetSymbol ?: return
        val superProperty = reference
            .type
            .classOrFail
            .owner
            .declarations
            .filterIsInstance<IrProperty>()
            .single { it.name == StandardNames.NAME }  // In K/Wasm interfaces can have fake overridden properties from Any

        val supperGetter = superProperty.getter
            ?: compilationException(
                "Expected getter for KFunction.name property",
                superProperty
            )

        val nameProperty = clazz.addProperty() {
            visibility = superProperty.visibility
            name = superProperty.name
            origin = GENERATED_MEMBER_IN_CALLABLE_REFERENCE
        }

        nameProperty.overriddenSymbols = listOf(superProperty.symbol)

        val getter = nameProperty.addGetter() {
            returnType = stringType
        }
        getter.overriddenSymbols = listOf(supperGetter.symbol)
        getter.parameters += getter.createDispatchReceiverParameterWithClassParent()

        // TODO: What name should be in case of constructor? <init> or class name?
        getter.body = context.irFactory.createBlockBody(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                IrReturnImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, nothingType, getter.symbol, IrConstImpl.string(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, stringType, reflectionTargetSymbol.owner.name.asString()
                    )
                )
            )
        )

        clazz.reflectedNameAccessor = getter
    }

    override fun generateExtraMethods(functionReferenceClass: IrClass, reference: IrRichFunctionReference) {
        if (reference.isKReference) {
            createNameProperty(functionReferenceClass, reference)
        }
    }

    companion object {
        val LAMBDA_IMPL by IrDeclarationOriginImpl
        val FUNCTION_REFERENCE_IMPL by IrDeclarationOriginImpl
        val GENERATED_MEMBER_IN_CALLABLE_REFERENCE by IrDeclarationOriginImpl
    }
}
