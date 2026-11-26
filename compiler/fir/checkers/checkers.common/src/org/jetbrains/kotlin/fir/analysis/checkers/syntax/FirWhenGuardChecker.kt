/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.PositioningStrategyKinds
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirWhenExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression

object FirWhenGuardChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        if (expression.source == null) return

        for (branch in expression.branches) {
            checkBranch(expression, branch)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkBranch(
        element: FirWhenExpression,
        branch: FirWhenBranch,
    ) {
        if (!branch.hasGuard) return
        val source = branch.source ?: return

        source.requireFeatureSupport(
            LanguageFeature.WhenGuards,
            positioningStrategyKind = PositioningStrategyKinds.WHEN_GUARD,
        )

        if (element.subjectVariable == null) {
            reporter.reportOn(source, FirErrors.WHEN_GUARD_WITHOUT_SUBJECT)
        } else {
            if (withSyntax { source.getChild(KotlinSyntaxTokenIds.COMMA, depth = 1) } != null) {
                reporter.reportOn(source, FirErrors.COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD)
            }
        }
    }
}
