/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors.UNSUPPORTED_FEATURE
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

object UnsupportedUntilOperatorChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val calleeExpression = resolvedCall.call.calleeExpression as? KtOperationReferenceExpression ?: return
        val isRangeUntilOperatorSupported = context.languageVersionSettings.supportsFeature(LanguageFeature.RangeUntilOperator)

        if (calleeExpression.operationSignTokenType == KtTokens.RANGE_UNTIL && !isRangeUntilOperatorSupported) {
            context.trace.report(
                UNSUPPORTED_FEATURE.on(reportOn, LanguageFeature.RangeUntilOperator to context.languageVersionSettings)
            )
        }
    }
}