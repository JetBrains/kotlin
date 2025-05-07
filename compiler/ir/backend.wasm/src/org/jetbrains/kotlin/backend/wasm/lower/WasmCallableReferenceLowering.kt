/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.Name
import kotlin.collections.plus
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSources.File as PLFile

class WasmCallableReferenceLowering(val backendContext: WasmBackendContext) : CallableReferenceLowering(backendContext) {
    override fun getClassOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin = FUNCTION_REFERENCE_IMPL

    override fun IrBuilderWithScope.generateSuperClassConstructorCall(
        constructor: IrConstructor,
        superClassType: IrType,
        functionReference: IrRichFunctionReference,
    ): IrDelegatingConstructorCall {
        return irDelegatingConstructorCall(superClassType.classOrFail.owner.primaryConstructor!!).apply {
            val linkerError = functionReference.getLinkageErrorIfAny(backendContext)
            when {
                linkerError != null -> {
                    arguments[0] = linkerError.toIrConst(context.irBuiltIns.stringType)
                }
                functionReference.reflectionTargetSymbol != null -> {
                    arguments[0] = functionReference.getFlags().toIrConst(context.irBuiltIns.intType)
                    arguments[1] = functionReference.getArity().toIrConst(context.irBuiltIns.intType)
                    arguments[2] = functionReference.getFqName(backendContext).toIrConst(context.irBuiltIns.stringType)
                }
            }
        }
    }

    override fun getSuperClassType(reference: IrRichFunctionReference): IrType = when {
        reference.reflectionTargetLinkageError != null -> backendContext.wasmSymbols.reflectionSymbols.kFunctionErrorImpl.defaultType
        reference.reflectionTargetSymbol != null -> backendContext.wasmSymbols.reflectionSymbols.kFunctionImpl.defaultType
        else -> backendContext.irBuiltIns.anyType
    }

    override fun generateExtraMethods(functionReferenceClass: IrClass, reference: IrRichFunctionReference) {
        super.generateExtraMethods(functionReferenceClass, reference)
        if (reference.reflectionTargetLinkageError != null) return
        if (reference.reflectionTargetSymbol == null) return
        if (reference.boundValues.isEmpty()) return

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

private fun IrRichFunctionReference.getFqName(backendContext: WasmBackendContext): String = when {
    isFunInterfaceConstructorAdapter() -> invokeFunction.returnType.getClass()!!.fqNameForIrSerialization.toString()
    else -> (backendContext.irFactory as IrFactoryImplForWasmIC).declarationSignature(reflectionTargetSymbol!!.owner).toString()
}


private fun IrRichFunctionReference.isFunInterfaceConstructorAdapter() =
    invokeFunction.origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR

private fun IrRichFunctionReference.getLinkageErrorIfAny(backendContext: WasmBackendContext): String? =
    reflectionTargetLinkageError?.let { reflectionTargetLinkageError ->
        backendContext.partialLinkageSupport.prepareLinkageError(
            doNotLog = false,
            reflectionTargetLinkageError,
            this,
            PLFile.determineFileFor(invokeFunction),
        )
    }