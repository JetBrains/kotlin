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

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.coroutines.hasSuspendFunctionType
import org.jetbrains.kotlin.coroutines.isSuspendLambda
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.hasRestrictSuspensionAnnotation
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object CoroutineSuspendCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor as? SimpleFunctionDescriptor ?: return
        if (!descriptor.isSuspend) return

        val (closestSuspendLambdaScope, closestSuspensionLambdaDescriptor) =
                context.scope
                        .parentsWithSelf.firstOrNull {
                                it is LexicalScope && it.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE &&
                                    it.ownerDescriptor.safeAs<CallableDescriptor>()?.isSuspendLambda == true
                        }?.let { it to it.cast<LexicalScope>().ownerDescriptor.cast<CallableDescriptor>() }
                ?: null to null

        val enclosingSuspendFunction =
                context.scope.parentsWithSelf.filterIsInstance<LexicalScope>().takeWhile { it != closestSuspendLambdaScope }
                        .firstOrNull {
                            (it.ownerDescriptor as? FunctionDescriptor)?.isSuspend == true
                        }?.ownerDescriptor as? SimpleFunctionDescriptor

        when {
            enclosingSuspendFunction != null -> {
                // Tail calls checks happen during control flow analysis
                // Here we only record enclosing function mapping (for backends purposes)
                context.trace.record(BindingContext.ENCLOSING_SUSPEND_FUNCTION_FOR_SUSPEND_FUNCTION_CALL, resolvedCall.call, enclosingSuspendFunction)

                checkRestrictSuspension(enclosingSuspendFunction.extensionReceiverParameter, resolvedCall, reportOn, context)
            }
            closestSuspensionLambdaDescriptor != null -> {
                val callElement = resolvedCall.call.callElement as KtExpression

                if (!InlineUtil.checkNonLocalReturnUsage(closestSuspensionLambdaDescriptor, callElement, context.resolutionContext)) {
                    context.trace.report(Errors.NON_LOCAL_SUSPENSION_POINT.on(reportOn))
                }

                context.trace.record(
                        BindingContext.ENCLOSING_SUSPEND_LAMBDA_FOR_SUSPENSION_POINT, resolvedCall.call, closestSuspensionLambdaDescriptor
                )

                checkRestrictSuspension(closestSuspensionLambdaDescriptor.extensionReceiverParameter, resolvedCall, reportOn, context)
            }
            else -> {
                context.trace.report(Errors.ILLEGAL_SUSPEND_FUNCTION_CALL.on(reportOn))
            }
        }
    }
}

object BuilderFunctionsCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor as? FunctionDescriptor ?: return
        if (descriptor.valueParameters.any { it.hasSuspendFunctionType }) {
            checkCoroutinesFeature(context.languageVersionSettings, context.trace, reportOn)
        }
    }
}

fun checkCoroutinesFeature(languageVersionSettings: LanguageVersionSettings, diagnosticHolder: DiagnosticSink, reportOn: PsiElement) {
    if (!languageVersionSettings.supportsFeature(LanguageFeature.Coroutines)) {
        diagnosticHolder.report(Errors.UNSUPPORTED_FEATURE.on(reportOn, LanguageFeature.Coroutines))
    }
    else if (languageVersionSettings.supportsFeature(LanguageFeature.ErrorOnCoroutines)) {
        diagnosticHolder.report(Errors.EXPERIMENTAL_FEATURE_ERROR.on(reportOn, LanguageFeature.Coroutines))
    }
    else if (languageVersionSettings.supportsFeature(LanguageFeature.WarnOnCoroutines)) {
        diagnosticHolder.report(Errors.EXPERIMENTAL_FEATURE_WARNING.on(reportOn, LanguageFeature.Coroutines))
    }
}

private fun checkRestrictSuspension(
        enclosingSuspendReceiver: ReceiverParameterDescriptor?,
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
) {
    if (enclosingSuspendReceiver == null) return
    val enclosingSuspendReceiverValue = enclosingSuspendReceiver.value

    fun ReceiverValue.isRestrictSuspensionReceiver() = (type.supertypes() + type).any {
        it.constructor.declarationDescriptor?.hasRestrictSuspensionAnnotation() == true
    }

    // todo explicit this and implicit this is not equals now
    infix fun ReceiverValue.sameInstance(other: ReceiverValue?) = this === other

    if (!enclosingSuspendReceiverValue.isRestrictSuspensionReceiver()) return

    // member of suspend receiver
    if (enclosingSuspendReceiverValue sameInstance resolvedCall.dispatchReceiver) return

    if (enclosingSuspendReceiverValue sameInstance resolvedCall.extensionReceiver &&
        resolvedCall.candidateDescriptor.extensionReceiverParameter!!.value.isRestrictSuspensionReceiver()) return

    context.trace.report(Errors.ILLEGAL_RESTRICTED_SUSPENDING_FUNCTION_CALL.on(reportOn))
}