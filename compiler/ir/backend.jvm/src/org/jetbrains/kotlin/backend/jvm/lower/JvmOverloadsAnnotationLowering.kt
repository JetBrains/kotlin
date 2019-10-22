/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.allTypeParameters
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_OVERLOADS_FQ_NAME

internal val jvmOverloadsAnnotationPhase = makeIrFilePhase(
    ::JvmOverloadsAnnotationLowering,
    name = "JvmOverloadsAnnotation",
    description = "Handle JvmOverloads annotations"
)

// TODO: `IrValueParameter.defaultValue` property does not track default values in super-parameters. See KT-28637.

private class JvmOverloadsAnnotationLowering(val context: JvmBackendContext) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val functions = irClass.declarations.filterIsInstance<IrFunction>().filter {
            it.hasAnnotation(JVM_OVERLOADS_FQ_NAME)
        }

        functions.forEach {
            generateWrappers(it, irClass)
        }
    }

    private fun generateWrappers(target: IrFunction, irClass: IrClass) {
        val numDefaultParameters = target.valueParameters.count { it.defaultValue != null }
        for (i in 0 until numDefaultParameters) {
            val wrapper = generateWrapper(target, i)
            irClass.addMember(wrapper)
        }
    }

    private fun generateWrapper(target: IrFunction, numDefaultParametersToExpect: Int): IrFunction {
        val wrapperIrFunction = generateWrapperHeader(target, numDefaultParametersToExpect)

        val call = if (target is IrConstructor)
            IrDelegatingConstructorCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, target.symbol)
        else
            IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.returnType, target.symbol)
        for (arg in wrapperIrFunction.allTypeParameters) {
            call.putTypeArgument(arg.index, arg.defaultType)
        }
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
            if (valueParameter.defaultValue != null) {
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

        wrapperIrFunction.body = if (target is IrConstructor) {
            IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(call))
        } else {
            IrExpressionBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, call
            )
        }

        return wrapperIrFunction
    }

    private fun generateWrapperHeader(oldFunction: IrFunction, numDefaultParametersToExpect: Int): IrFunction {
        val res = when (oldFunction) {
            is IrConstructor -> {
                val descriptor = WrappedClassConstructorDescriptor(oldFunction.descriptor.annotations)
                IrConstructorImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER,
                    IrConstructorSymbolImpl(descriptor),
                    oldFunction.name,
                    oldFunction.visibility,
                    returnType = oldFunction.returnType,
                    isInline = oldFunction.isInline,
                    isExternal = false,
                    isPrimary = false,
                    isExpect = false
                ).apply {
                    descriptor.bind(this)
                }
            }
            is IrSimpleFunction -> {
                val descriptor = WrappedSimpleFunctionDescriptor(oldFunction.descriptor.annotations)
                IrFunctionImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER,
                    IrSimpleFunctionSymbolImpl(descriptor),
                    oldFunction.name,
                    oldFunction.visibility,
                    oldFunction.modality,
                    returnType = oldFunction.returnType,
                    isInline = oldFunction.isInline,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = oldFunction.isSuspend,
                    isExpect = false,
                    isFakeOverride = false
                ).apply {
                    descriptor.bind(this)
                }
            }
            else -> error("Unknown kind of IrFunction: $oldFunction")
        }

        res.parent = oldFunction.parent
        res.annotations.addAll(oldFunction.annotations.map { it.deepCopyWithSymbols(res) })
        res.copyTypeParametersFrom(oldFunction)
        res.dispatchReceiverParameter = oldFunction.dispatchReceiverParameter?.copyTo(res)
        res.extensionReceiverParameter = oldFunction.extensionReceiverParameter?.copyTo(res)
        res.valueParameters.addAll(res.generateNewValueParameters(oldFunction, numDefaultParametersToExpect))
        return res
    }

    private fun IrFunction.generateNewValueParameters(
        oldFunction: IrFunction,
        numDefaultParametersToExpect: Int
    ): List<IrValueParameter> {
        var parametersCopied = 0
        var defaultParametersCopied = 0
        val result = mutableListOf<IrValueParameter>()
        for (oldValueParameter in oldFunction.valueParameters) {
            if (oldValueParameter.defaultValue != null &&
                defaultParametersCopied < numDefaultParametersToExpect
            ) {
                defaultParametersCopied++
                result.add(
                    oldValueParameter.copyTo(
                        this,
                        index = parametersCopied++,
                        defaultValue = null,
                        isCrossinline = oldValueParameter.isCrossinline,
                        isNoinline = oldValueParameter.isNoinline
                    )
                )
            } else if (oldValueParameter.defaultValue == null) {
                result.add(oldValueParameter.copyTo(this, index = parametersCopied++))
            }
        }
        return result
    }
}