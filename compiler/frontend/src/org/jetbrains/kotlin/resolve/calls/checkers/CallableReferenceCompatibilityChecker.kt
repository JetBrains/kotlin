/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isCallableReference
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class CallableReferenceCompatibilityChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val typeInferenceForCallableReferencesFeature = LanguageFeature.TypeInferenceOnGenericsForCallableReferences
        if (context.languageVersionSettings.supportsFeature(typeInferenceForCallableReferencesFeature)) return

        for ((_, resolvedArgument) in resolvedCall.valueArguments) {
            inner@ for (argument in resolvedArgument.arguments) {
                val argumentExpression = argument.getArgumentExpression() as? KtCallableReferenceExpression ?: continue@inner
                val callableReferenceResolvedCall = argumentExpression.callableReference.getResolvedCall(context.trace.bindingContext) ?: continue@inner
                if (callableReferenceResolvedCall.call.isCallableReference() &&
                    callableReferenceResolvedCall.candidateDescriptor.typeParameters.isNotEmpty()) {
                    context.trace.report(Errors.UNSUPPORTED_FEATURE.on(argumentExpression,
                                                                       typeInferenceForCallableReferencesFeature to context.languageVersionSettings))
                }
            }
        }
    }
}