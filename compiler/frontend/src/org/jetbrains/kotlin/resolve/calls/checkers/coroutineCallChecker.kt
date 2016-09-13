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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.coroutine.CoroutineReceiverValue
import org.jetbrains.kotlin.resolve.inline.InlineUtil

object CoroutineSuspendCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor as? FunctionDescriptor ?: return
        if (!descriptor.isSuspend || descriptor.initialSignatureDescriptor == null) return

        val dispatchReceiverOwner = (resolvedCall.dispatchReceiver as? CoroutineReceiverValue)?.declarationDescriptor ?: return
        val callElement = resolvedCall.call.callElement as KtExpression

        if (!InlineUtil.checkNonLocalReturnUsage(dispatchReceiverOwner, callElement, context.resolutionContext)) {
            context.trace.report(Errors.NON_LOCAL_SUSPENSION_POINT.on(reportOn))
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
