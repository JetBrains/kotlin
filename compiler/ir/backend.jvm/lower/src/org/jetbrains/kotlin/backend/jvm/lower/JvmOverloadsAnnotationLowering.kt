/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyAnnotationsFrom
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.allTypeParameters
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.JvmNames.JVM_OVERLOADS_FQ_NAME

// TODO: `IrValueParameter.defaultValue` property does not track default values in super-parameters. See KT-28637.

internal class JvmOverloadsAnnotationLowering(val context: JvmBackendContext) : ClassLoweringPass {

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
        for (i in numDefaultParameters - 1 downTo 0) {
            val wrapper = generateWrapper(target, i)
            irClass.addMember(wrapper)
        }
    }

    private fun generateWrapper(target: IrFunction, numDefaultParametersToExpect: Int): IrFunction {
        val wrapperIrFunction = context.irFactory.generateWrapperHeader(target, numDefaultParametersToExpect)

        val call = when (target) {
            is IrConstructor ->
                IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, target.symbol
                )
            is IrSimpleFunction ->
                IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.returnType, target.symbol)
            else ->
                error("unknown function kind: ${target.render()}")
        }
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

    private fun IrFactory.generateWrapperHeader(oldFunction: IrFunction, numDefaultParametersToExpect: Int): IrFunction {
        val res = when (oldFunction) {
            is IrConstructor -> {
                buildConstructor {
                    origin = JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
                    name = oldFunction.name
                    visibility = oldFunction.visibility
                    returnType = oldFunction.returnType
                    isInline = oldFunction.isInline
                }
            }
            is IrSimpleFunction -> buildFun {
                origin = JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
                name = oldFunction.name
                visibility = oldFunction.visibility
                modality =
                    if (context.state.languageVersionSettings.supportsFeature(LanguageFeature.GenerateJvmOverloadsAsFinal)) Modality.FINAL
                    else oldFunction.modality
                returnType = oldFunction.returnType
                isInline = oldFunction.isInline
                isSuspend = oldFunction.isSuspend
            }
            else -> error("Unknown kind of IrFunction: $oldFunction")
        }

        res.parent = oldFunction.parent
        res.copyAnnotationsFrom(oldFunction)
        res.copyTypeParametersFrom(oldFunction)
        res.dispatchReceiverParameter = oldFunction.dispatchReceiverParameter?.copyTo(res)
        res.extensionReceiverParameter = oldFunction.extensionReceiverParameter?.copyTo(res)
        res.valueParameters += res.generateNewValueParameters(oldFunction, numDefaultParametersToExpect)
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
