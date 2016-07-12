/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.isCaptured
import org.jetbrains.kotlin.resolve.calls.inference.wrapWithCapturingSubstitution
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing

fun ResolutionContext<*>.reportTypeMismatchDueToTypeProjection(
        expression: KtElement,
        expectedType: KotlinType,
        expressionType: KotlinType?
): Boolean {
    if (!TypeUtils.contains(expectedType) { it.isAnyOrNullableAny() || it.isNothing() || it.isNullableNothing() }) return false

    val callPosition = this.callPosition
    val (resolvedCall, correspondingNotApproximatedTypeByDescriptor: (CallableDescriptor) -> KotlinType?) = when (callPosition) {
        is CallPosition.ValueArgumentPosition -> Pair(
                callPosition.resolvedCall, {
                    f: CallableDescriptor ->
                    getEffectiveExpectedType(f.valueParameters[callPosition.valueParameter.index], callPosition.valueArgument)
                })
        is CallPosition.ExtensionReceiverPosition -> Pair(
                callPosition.resolvedCall, {
                    f: CallableDescriptor ->
                    f.extensionReceiverParameter?.type
                })
        is CallPosition.PropertyAssignment -> Pair(
                callPosition.leftPart.getResolvedCall(trace.bindingContext) ?: return false, {
                    f: CallableDescriptor ->
                    (f as? PropertyDescriptor)?.setter?.valueParameters?.get(0)?.type
                })
        is CallPosition.Unknown -> return false
    }

    val receiverType = resolvedCall.smartCastDispatchReceiverType
                       ?: (resolvedCall.dispatchReceiver ?: return false).type

    val callableDescriptor = resolvedCall.resultingDescriptor.original

    val substitutedDescriptor =
            TypeConstructorSubstitution
                    .create(receiverType)
                    .wrapWithCapturingSubstitution(needApproximation = false)
                    .buildSubstitutor().let { callableDescriptor.substitute(it) } ?: return false

    val nonApproximatedExpectedType = correspondingNotApproximatedTypeByDescriptor(substitutedDescriptor) ?: return false
    if (!TypeUtils.contains(nonApproximatedExpectedType) { it.isCaptured() }) return false

    if (expectedType.isNothing()) {
        if (callPosition is CallPosition.PropertyAssignment) {
            trace.report(Errors.SETTER_PROJECTED_OUT.on(callPosition.leftPart ?: return false, resolvedCall.resultingDescriptor))
        }
        else {
            val call = resolvedCall.call
            val reportOn =
                    if (resolvedCall is VariableAsFunctionResolvedCall)
                        resolvedCall.variableCall.call.calleeExpression
                    else
                        call.calleeExpression

            trace.reportDiagnosticOnce(Errors.MEMBER_PROJECTED_OUT.on(reportOn ?: call.callElement, callableDescriptor, receiverType))
        }
    }
    else {
        // expressionType can be null when reporting CONSTANT_EXPECTED_TYPE_MISMATCH (see addAll.kt test)
        expressionType ?: return false
        trace.report(
                Errors.TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS.on(
                        expression, TypeMismatchDueToTypeProjectionsData(
                        expectedType, expressionType, receiverType, callableDescriptor)))

    }

    return true
}

private fun BindingTrace.reportDiagnosticOnce(diagnostic: Diagnostic) {
    if (bindingContext.diagnostics.forElement(diagnostic.psiElement).any { it.factory == diagnostic.factory }) return

    report(diagnostic)
}

class TypeMismatchDueToTypeProjectionsData(
        val expectedType: KotlinType,
        val expressionType: KotlinType,
        val receiverType: KotlinType,
        val callableDescriptor: CallableDescriptor
)

fun ResolutionContext<*>.reportTypeMismatchDueToScalaLikeNamedFunctionSyntax(
        expression: KtElement,
        expectedType: KotlinType,
        expressionType: KotlinType?
): Boolean {
    if (expressionType == null) return false

    if (expressionType.isFunctionType && !expectedType.isFunctionType && isScalaLikeEqualsBlock(expression)) {
        trace.report(Errors.TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN.on(expression, expectedType))
        return true
    }

    return false
}

private fun isScalaLikeEqualsBlock(expression: KtElement): Boolean =
        expression is KtLambdaExpression &&
        expression.parent.let { it is KtNamedFunction && it.equalsToken != null }
