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

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageFeatureSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.coroutine.CoroutineReceiverValue
import org.jetbrains.kotlin.resolve.inline.InlineUtil

object CoroutineSuspendCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, context: BasicCallResolutionContext) {
        val descriptor = resolvedCall.candidateDescriptor as? FunctionDescriptor ?: return
        if (!descriptor.isSuspend || descriptor.initialSignatureDescriptor == null) return

        val dispatchReceiverOwner = (resolvedCall.dispatchReceiver as? CoroutineReceiverValue)?.declarationDescriptor ?: return
        val callElement = resolvedCall.call.callElement as KtExpression

        if (!InlineUtil.checkNonLocalReturnUsage(dispatchReceiverOwner, callElement, context.trace)) {
            context.trace.report(Errors.NON_LOCAL_SUSPENSION_POINT.on(resolvedCall.call.calleeExpression ?: callElement))
        }
    }
}

// It can't be another CallChecker implementation because of 3rd parameter
fun checkCoroutineBuilderCall(
        resolvedCall: ResolvedCall<*>,
        context: BasicCallResolutionContext,
        languageFeatureSettings: LanguageFeatureSettings
) {
    val descriptor = resolvedCall.candidateDescriptor as? FunctionDescriptor ?: return
    if (descriptor.valueParameters.any { it.isCoroutine }
        && !languageFeatureSettings.supportsFeature(LanguageFeature.Coroutines)) {
        context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(
                        resolvedCall.call.calleeExpression ?: resolvedCall.call.callElement, LanguageFeature.Coroutines))
    }
}

