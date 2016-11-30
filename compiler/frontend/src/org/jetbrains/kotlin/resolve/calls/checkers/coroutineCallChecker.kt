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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.coroutine.CoroutineReceiverValue
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf

object CoroutineSuspendCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor as? SimpleFunctionDescriptor ?: return
        if (!descriptor.isSuspend) return

        val closestCoroutineReceiver =
                context.scope.getImplicitReceiversHierarchy().firstOrNull { it.value is CoroutineReceiverValue }?.value as CoroutineReceiverValue?

        val enclosingSuspendFunction =
                context.scope.parentsWithSelf.filterIsInstance<LexicalScope>().takeWhile {
                    closestCoroutineReceiver == null || it.implicitReceiver?.value != closestCoroutineReceiver
                }.firstOrNull {
                    (it.ownerDescriptor as? FunctionDescriptor)?.isSuspend == true
                }?.ownerDescriptor as? SimpleFunctionDescriptor

        when {
            enclosingSuspendFunction != null -> {
                // Tail calls checks happen during control flow analysis
                // Here we only record enclosing function mapping (for backends purposes)
                context.trace.record(BindingContext.ENCLOSING_SUSPEND_FUNCTION_FOR_SUSPEND_FUNCTION_CALL, resolvedCall.call, enclosingSuspendFunction)
            }
            closestCoroutineReceiver != null -> {
                val callElement = resolvedCall.call.callElement as KtExpression

                if (!InlineUtil.checkNonLocalReturnUsage(closestCoroutineReceiver.declarationDescriptor, callElement, context.resolutionContext)) {
                    context.trace.report(Errors.NON_LOCAL_SUSPENSION_POINT.on(reportOn))
                }

                context.trace.record(BindingContext.COROUTINE_RECEIVER_FOR_SUSPENSION_POINT, resolvedCall.call, closestCoroutineReceiver)
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
        if (descriptor.valueParameters.any { it.isCoroutine } &&
            !context.languageVersionSettings.supportsFeature(LanguageFeature.Coroutines)) {
            context.trace.report(Errors.UNSUPPORTED_FEATURE.on(reportOn, LanguageFeature.Coroutines))
        }
    }
}
