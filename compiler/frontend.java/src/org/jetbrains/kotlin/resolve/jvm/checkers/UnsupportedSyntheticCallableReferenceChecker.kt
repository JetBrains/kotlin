/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors.UNSUPPORTED_FEATURE
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotationsDeeply
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.extractCallableReferenceExpression
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

/**
 * This is K1 implementation.
 * For K2 implementation see: [org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression.FirUnsupportedSyntheticCallableReferenceChecker]
 */
class UnsupportedSyntheticCallableReferenceChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        // TODO: support references to synthetic Java extension properties (KT-8575)
        val callableReferenceExpression = resolvedCall.call.extractCallableReferenceExpression() ?: return

        // We allow resolution of top-level callable references to synthetic Java extension properties in the delegate position. See KT-47299
        if (callableReferenceExpression.unwrapParenthesesLabelsAndAnnotationsDeeply() is KtPropertyDelegate) return

        if (resolvedCall.resultingDescriptor is SyntheticJavaPropertyDescriptor) {
            if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ReferencesToSyntheticJavaProperties)) {
                context.trace.report(UNSUPPORTED_FEATURE.on(reportOn, LanguageFeature.ReferencesToSyntheticJavaProperties to context.languageVersionSettings))
            } else if (!context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) {
                context.trace.report(UNSUPPORTED_FEATURE.on(reportOn, LanguageFeature.NewInference to context.languageVersionSettings))
            }
        }
    }
}
