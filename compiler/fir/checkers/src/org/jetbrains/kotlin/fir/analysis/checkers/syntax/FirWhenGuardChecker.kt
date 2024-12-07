/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.lexer.KtTokens

object FirWhenGuardChecker : FirExpressionSyntaxChecker<FirWhenExpression, PsiElement>() {
    override fun checkPsiOrLightTree(
        element: FirWhenExpression,
        source: KtSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        for (branch in element.branches) {
            checkBranch(element, branch, context, reporter)
        }
    }

    private fun checkBranch(
        element: FirWhenExpression,
        branch: FirWhenBranch,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (!branch.hasGuard) return
        val source = branch.source ?: return

        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.WhenGuards)) {
            reporter.reportOn(
                source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.WhenGuards to context.session.languageVersionSettings,
                context,
                positioningStrategy = SourceElementPositioningStrategies.WHEN_GUARD,
            )
        }

        if (element.subject == null) {
            reporter.reportOn(source, FirErrors.WHEN_GUARD_WITHOUT_SUBJECT, context)
        } else {
            if (source.getChild(KtTokens.COMMA, depth = 1) != null) {
                reporter.reportOn(source, FirErrors.COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD, context)
            }
        }
    }
}
