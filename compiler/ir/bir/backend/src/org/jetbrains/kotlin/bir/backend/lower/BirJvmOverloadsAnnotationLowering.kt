/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.builders.build
import org.jetbrains.kotlin.bir.builders.setCall
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirCall
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpression
import org.jetbrains.kotlin.bir.expressions.impl.BirBlockBodyImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirDelegatingConstructorCallImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirExpressionBodyImpl
import org.jetbrains.kotlin.bir.expressions.impl.BirGetValueImpl
import org.jetbrains.kotlin.bir.types.utils.defaultType
import org.jetbrains.kotlin.bir.util.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.JvmStandardClassIds

context(JvmBirBackendContext)
class BirJvmOverloadsAnnotationLowering : BirLoweringPhase() {
    private val JvmOverloadsAnnotation = birBuiltIns.findClass(JvmStandardClassIds.JVM_OVERLOADS_FQ_NAME)

    private val overloadsAnnotations = registerIndexKey<BirConstructorCall>(false) {
        it.constructedClass == JvmOverloadsAnnotation
    }

    override fun invoke(module: BirModuleFragment) {
        compiledBir.getElementsWithIndex(overloadsAnnotations).forEach { annotation ->
            val function = annotation.parent as? BirFunction ?: return@forEach
            val parentClass = function.parent as? BirClass ?: return@forEach

            generateWrappers(function, parentClass)
        }
    }

    private fun generateWrappers(target: BirFunction, irClass: BirClass) {
        val numDefaultParameters = target.valueParameters.count { it.defaultValue != null }
        for (i in numDefaultParameters - 1 downTo 0) {
            val wrapper = generateWrapper(target, i)
            irClass.declarations += wrapper
        }
    }

    private fun generateWrapper(target: BirFunction, numDefaultParametersToExpect: Int): BirFunction {
        val wrapperBirFunction = generateWrapperHeader(target, numDefaultParametersToExpect)

        val call = when (target) {
            is BirConstructor -> BirDelegatingConstructorCallImpl(
                sourceSpan = SourceSpan.UNDEFINED,
                type = birBuiltIns.unitType,
                symbol = target,
                dispatchReceiver = null,
                extensionReceiver = null,
                origin = null,
                typeArguments = emptyList(),
                contextReceiversCount = 0
            )
            is BirSimpleFunction -> BirCall.build {
                setCall(target)
            }
            else -> error("unknown function kind: ${target.render()}")
        }

        call.typeArguments = wrapperBirFunction.allTypeParameters.map { it.defaultType }

        call.dispatchReceiver = wrapperBirFunction.dispatchReceiverParameter?.let { dispatchReceiver ->
            BirGetValueImpl(SourceSpan.UNDEFINED, dispatchReceiver.type, dispatchReceiver, null)
        }
        call.extensionReceiver = wrapperBirFunction.extensionReceiverParameter?.let { extensionReceiver ->
            BirGetValueImpl(SourceSpan.UNDEFINED, extensionReceiver.type, extensionReceiver, null)
        }

        call.valueArguments += List<BirExpression?>(target.valueParameters.size) { null }
        var parametersCopied = 0
        var defaultParametersCopied = 0
        for ((i, valueParameter) in target.valueParameters.withIndex()) {
            if (valueParameter.defaultValue != null) {
                if (defaultParametersCopied < numDefaultParametersToExpect) {
                    defaultParametersCopied++
                    val arg = wrapperBirFunction.valueParameters[parametersCopied++]
                    call.valueArguments[i] = BirGetValueImpl(SourceSpan.UNDEFINED, arg.type, arg, null)
                }
            } else {
                val arg = wrapperBirFunction.valueParameters[parametersCopied++]
                call.valueArguments[i] = BirGetValueImpl(SourceSpan.UNDEFINED, arg.type, arg, null)
            }
        }

        wrapperBirFunction.body = if (target is BirConstructor) {
            BirBlockBodyImpl(SourceSpan.UNDEFINED).apply {
                statements += call
            }
        } else {
            BirExpressionBodyImpl(SourceSpan.UNDEFINED, call)
        }

        return wrapperBirFunction
    }

    private fun generateWrapperHeader(oldFunction: BirFunction, numDefaultParametersToExpect: Int): BirFunction {
        val res = when (oldFunction) {
            is BirConstructor -> {
                BirConstructor.build {
                    origin = JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
                    name = oldFunction.name
                    visibility = oldFunction.visibility
                    returnType = oldFunction.returnType
                    isInline = oldFunction.isInline
                }
            }
            is BirSimpleFunction -> BirSimpleFunction.build {
                origin = JvmLoweredDeclarationOrigin.JVM_OVERLOADS_WRAPPER
                name = oldFunction.name
                visibility = oldFunction.visibility
                modality =
                    if (languageVersionSettings.supportsFeature(LanguageFeature.GenerateJvmOverloadsAsFinal)) Modality.FINAL
                    else oldFunction.modality
                returnType = oldFunction.returnType
                isInline = oldFunction.isInline
                isSuspend = oldFunction.isSuspend
            }
            else -> error("Unknown kind of BirFunction: $oldFunction")
        }

        res.annotations += oldFunction.copyAnnotations()
        res.copyTypeParametersFrom(oldFunction)
        res.dispatchReceiverParameter = oldFunction.dispatchReceiverParameter?.copyTo(res)
        res.extensionReceiverParameter = oldFunction.extensionReceiverParameter?.copyTo(res)
        res.valueParameters += res.generateNewValueParameters(oldFunction, numDefaultParametersToExpect)
        return res
    }

    private fun BirFunction.generateNewValueParameters(
        oldFunction: BirFunction,
        numDefaultParametersToExpect: Int,
    ): List<BirValueParameter> {
        var parametersCopied = 0
        var defaultParametersCopied = 0
        val result = mutableListOf<BirValueParameter>()
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
                    )
                )
            } else if (oldValueParameter.defaultValue == null) {
                result.add(oldValueParameter.copyTo(this, index = parametersCopied++))
            }
        }
        return result
    }
}