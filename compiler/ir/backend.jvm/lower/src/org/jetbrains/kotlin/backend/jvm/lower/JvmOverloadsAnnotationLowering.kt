/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_OVERLOADS_FQ_NAME
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Handles JvmOverloads annotations.
 *
 * Note that [IrValueParameter.defaultValue] property does not track default values in super-parameters.
 * See [KT-28637](youtrack.jetbrains.com/issue/KT-28637).
 */
@PhasePrerequisites(JvmVersionOverloadsLowering::class)
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
        val numDefaultParameters = target.parameters.count { it.defaultValue != null }
        val hasIntroducedAt = target.parameters.any { it.hasAnnotation(StandardClassIds.Annotations.IntroducedAt) }

        for (i in numDefaultParameters - 1 downTo 0) {
            val wrapper = generateWrapper(target, i)

            if (!hasIntroducedAt || !irClass.hasConflictingOverloads(wrapper)) {
                irClass.addMember(wrapper)
            }
        }
    }

    private fun IrClass.hasConflictingOverloads(wrapper: IrFunction): Boolean {
        val signature = context.defaultMethodSignatureMapper.mapAsmMethod(wrapper)
        return functions.any {
            context.defaultMethodSignatureMapper.mapAsmMethod(it) == signature
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
        }
        for (arg in wrapperIrFunction.allTypeParameters) {
            call.typeArguments[arg.index] = arg.defaultType
        }

        var parametersCopied = 0
        var defaultParametersCopied = 0
        call.arguments.assignFrom(target.parameters) { valueParameter ->
            fun irGetParameter() =
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, wrapperIrFunction.parameters[parametersCopied++].symbol)

            when {
                valueParameter.defaultValue == null -> irGetParameter()
                defaultParametersCopied < numDefaultParametersToExpect -> {
                    defaultParametersCopied++
                    irGetParameter()
                }
                else -> null
            }
        }

        wrapperIrFunction.body = when (target) {
            is IrConstructor -> {
                context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(call))
            }
            is IrSimpleFunction -> {
                context.irFactory.createExpressionBody(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, call
                )
            }
        }

        return wrapperIrFunction
    }

    private fun IrFactory.generateWrapperHeader(oldFunction: IrFunction, numDefaultParametersToExpect: Int): IrFunction {
        val res = when (oldFunction) {
            is IrConstructor -> {
                buildConstructor {
                    setSourceRange(oldFunction)
                    origin = JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
                    name = oldFunction.name
                    visibility = oldFunction.visibility
                    returnType = oldFunction.returnType
                    isInline = oldFunction.isInline
                }
            }
            is IrSimpleFunction -> buildFun {
                setSourceRange(oldFunction)
                origin = JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
                name = oldFunction.name
                visibility = oldFunction.visibility
                modality =
                    if (context.config.languageVersionSettings.supportsFeature(LanguageFeature.GenerateJvmOverloadsAsFinal)) Modality.FINAL
                    else oldFunction.modality
                returnType = oldFunction.returnType
                isInline = oldFunction.isInline
                isSuspend = oldFunction.isSuspend
            }
        }

        res.parent = oldFunction.parent
        res.copyAnnotationsFrom(oldFunction)
        res.copyTypeParametersFrom(oldFunction)
        res.parameters += res.generateNewParameters(oldFunction, numDefaultParametersToExpect)
        return res
    }

    private fun IrFunction.generateNewParameters(
        oldFunction: IrFunction,
        numDefaultParametersToExpect: Int
    ): List<IrValueParameter> {
        var defaultParametersCopied = 0
        return oldFunction.parameters.mapNotNull { oldParameter ->
            when (oldParameter.defaultValue) {
                null -> oldParameter.copyTo(this)
                else if defaultParametersCopied < numDefaultParametersToExpect -> {
                    defaultParametersCopied++
                    oldParameter.copyTo(
                        this,
                        defaultValue = null,
                        isCrossinline = oldParameter.isCrossinline,
                        isNoinline = oldParameter.isNoinline
                    )
                }
                else -> null
            }
        }
    }
}
