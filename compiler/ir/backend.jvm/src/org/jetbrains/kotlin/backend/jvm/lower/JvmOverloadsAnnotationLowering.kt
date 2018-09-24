/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation


class JvmOverloadsAnnotationLowering(val context: JvmBackendContext) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val functions = irClass.declarations.filterIsInstance<IrFunction>().filter {
            it.descriptor.findJvmOverloadsAnnotation() != null
        }

        functions.forEach {
            generateWrappers(it, irClass)
        }
    }

    private fun generateWrappers(target: IrFunction, irClass: IrClass) {
        val numDefaultParameters = target.symbol.descriptor.valueParameters.count { it.hasDefaultValue() }
        for (i in 0 until numDefaultParameters) {
            val wrapper = generateWrapper(target, i)
            irClass.addMember(wrapper)
        }
    }

    private fun generateWrapper(target: IrFunction, numDefaultParametersToExpect: Int): IrFunction {
        val wrapperSymbol = generateWrapperSymbol(target.symbol, numDefaultParametersToExpect)
        val wrapperIrFunction = when (wrapperSymbol) {
            is IrConstructorSymbol -> IrConstructorImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER,
                wrapperSymbol
            )
            is IrSimpleFunctionSymbol -> IrFunctionImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER,
                wrapperSymbol
            )
            else -> error("expected IrConstructorSymbol or IrSimpleFunctionSymbol")
        }

        wrapperIrFunction.returnType = target.returnType
        wrapperIrFunction.createParameterDeclarations()

        val call = if (target is IrConstructor)
            IrDelegatingConstructorCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.returnType, target.symbol, target.descriptor)
        else
            IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.returnType, target.symbol)
        call.dispatchReceiver = wrapperIrFunction.dispatchReceiverParameter?.let { dispatchReceiver ->
            IrGetValueImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                dispatchReceiver.symbol
            )
        }
        call.extensionReceiver = wrapperIrFunction.extensionReceiverParameter?.let { extensionReceiver ->
            IrGetValueImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                extensionReceiver.symbol
            )
        }

        var parametersCopied = 0
        var defaultParametersCopied = 0
        for ((i, valueParameter) in target.valueParameters.withIndex()) {
            if ((valueParameter.descriptor as ValueParameterDescriptor).hasDefaultValue()) {
                if (defaultParametersCopied < numDefaultParametersToExpect) {
                    defaultParametersCopied++
                    call.putValueArgument(
                        i,
                        IrGetValueImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            wrapperIrFunction.valueParameters[parametersCopied++].symbol
                        )
                    )
                } else {
                    call.putValueArgument(i, null)
                }
            } else {
                call.putValueArgument(
                    i,
                    IrGetValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        wrapperIrFunction.valueParameters[parametersCopied++].symbol
                    )
                )
            }

        }

        wrapperIrFunction.body = IrExpressionBodyImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, call
        )

        target.annotations.mapTo(wrapperIrFunction.annotations) { it.deepCopyWithSymbols() }

        return wrapperIrFunction
    }

    private fun generateWrapperSymbol(oldSymbol: IrFunctionSymbol, numDefaultParametersToExpect: Int): IrFunctionSymbol {
        val oldDescriptor = oldSymbol.descriptor
        val newDescriptor = if (oldDescriptor is ClassConstructorDescriptor) {
            oldDescriptor.copyWithModifiedParameters(numDefaultParametersToExpect)
        } else {
            val newParameters = generateNewValueParameters(oldDescriptor, numDefaultParametersToExpect)
            oldDescriptor.newCopyBuilder()
                .setValueParameters(newParameters)
                .setOriginal(null)
                .setKind(CallableMemberDescriptor.Kind.SYNTHESIZED)
                .build()!!
        }

        return createFunctionSymbol(newDescriptor)
    }

    private fun ClassConstructorDescriptor.copyWithModifiedParameters(numDefaultParametersToExpect: Int): ClassConstructorDescriptor {
        val result = ClassConstructorDescriptorImpl.createSynthesized(
            this.containingDeclaration,
            annotations,
            /* isPrimary = */ false,
            source
        )
        // Call the long version of `initialize()`, because otherwise default implementation inserts
        // an unwanted `dispatchReceiverParameter`.
        result.initialize(
            extensionReceiverParameter?.copy(result),
            dispatchReceiverParameter,
            typeParameters,
            generateNewValueParameters(this, numDefaultParametersToExpect),
            returnType,
            modality,
            visibility
        )
        return result
    }

    private fun generateNewValueParameters(
        oldDescriptor: FunctionDescriptor,
        numDefaultParametersToExpect: Int
    ): List<ValueParameterDescriptor> {
        var parametersCopied = 0
        var defaultParametersCopied = 0
        val result = mutableListOf<ValueParameterDescriptor>()
        for (oldValueParameter in oldDescriptor.valueParameters) {
            if (oldValueParameter.hasDefaultValue() &&
                defaultParametersCopied < numDefaultParametersToExpect
            ) {
                defaultParametersCopied++
                result.add(
                    ValueParameterDescriptorImpl(
                        oldDescriptor,      // to be substituted with newDescriptor
                        null,
                        parametersCopied++,
                        oldValueParameter.annotations,
                        oldValueParameter.name,
                        oldValueParameter.type,
                        declaresDefaultValue = false,
                        isCrossinline = false,
                        isNoinline = false,
                        varargElementType = oldValueParameter.varargElementType,
                        source = oldValueParameter.source
                    )
                )
            } else if (!oldValueParameter.hasDefaultValue()) {
                result.add(oldValueParameter.copy(oldDescriptor, oldValueParameter.name, parametersCopied++))

            }
        }
        return result
    }
}