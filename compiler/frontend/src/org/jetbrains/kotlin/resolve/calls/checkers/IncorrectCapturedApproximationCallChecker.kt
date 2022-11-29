/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.inference.isCaptured
import org.jetbrains.kotlin.resolve.calls.inference.wrapWithCapturingSubstitution
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.util.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.sam.getFunctionTypeForSamType
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

object IncorrectCapturedApproximationCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (!resolvedCall.isReallySuccess()) return
        val dispatchReceiverType = resolvedCall.smartCastDispatchReceiverType ?: resolvedCall.dispatchReceiver?.type ?: return
        if (dispatchReceiverType.arguments.all { it.projectionKind == Variance.INVARIANT && !it.type.isCaptured() }) return

        val substitutor =
            TypeSubstitutor.create(dispatchReceiverType)
                .substitution.wrapWithCapturingSubstitution(needApproximation = false).buildSubstitutor()

        val functionDescriptor = resolvedCall.resultingDescriptor.original as? FunctionDescriptor ?: return

        val capturedSubstituted = functionDescriptor.substitute(substitutor) ?: return

        val indexedArguments = resolvedCall.valueArgumentsByIndex ?: return
        for ((index, parameter) in capturedSubstituted.valueParameters.withIndex()) {
            for (argument in indexedArguments[index].arguments) {
                val expectedType =
                    getEffectiveExpectedType(parameter, argument, context.resolutionContext)

                val argumentExpression = argument.getArgumentExpression() ?: continue
                val expressionType = argumentExpression.getType(context.trace.bindingContext) ?: continue

                val dataFlowValue =
                    context.dataFlowValueFactory.createDataFlowValue(argumentExpression, expressionType, context.resolutionContext)
                if (shouldWarningBeReported(expressionType, expectedType, dataFlowValue, context)) {
                    context.trace.report(
                        Errors.TYPE_MISMATCH_WARNING_FOR_INCORRECT_CAPTURE_APPROXIMATION.on(
                            argumentExpression, expectedType, expressionType
                        )
                    )
                }
            }
        }

        capturedSubstituted.extensionReceiverParameter?.let { extensionReceiverParameter ->
            val extensionReceiver = resolvedCall.extensionReceiver ?: return@let

            val dataFlowValue =
                context.dataFlowValueFactory.createDataFlowValue(extensionReceiver, context.resolutionContext)

            if (shouldWarningBeReported(extensionReceiver.type, extensionReceiverParameter.type, dataFlowValue, context)) {
                val expression = (extensionReceiver as? ExpressionReceiver)?.expression ?: reportOn
                context.trace.report(
                    Errors.RECEIVER_TYPE_MISMATCH_WARNING_FOR_INCORRECT_CAPTURE_APPROXIMATION.on(
                        expression, extensionReceiverParameter.type, extensionReceiver.type
                    )
                )
            }
        }
    }

    private fun shouldWarningBeReported(
        expressionType: KotlinType,
        expectedType: KotlinType,
        dataFlowValue: DataFlowValue,
        context: CallCheckerContext,
    ): Boolean {
        if (expectedType.arguments.none { arg -> !arg.isStarProjection && arg.type.contains(UnwrappedType::isCaptured) }) return false
        if (expressionType.isSubtypeOf(expectedType)) return false

        val samExpectedType = getFunctionTypeForSamType(
            expectedType, context.callComponents.samConversionResolver, context.callComponents.samConversionOracle,
        )

        if (expectedType.isFunctionOrSuspendFunctionType || samExpectedType?.isFunctionOrSuspendFunctionType == true) return false

        return context.dataFlowInfo.getCollectedTypes(
            dataFlowValue,
            context.languageVersionSettings,
        ).none { it.isSubtypeOf(expectedType) }
    }
}
