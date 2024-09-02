/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression

object FirCorrectGuardKeywordChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.subject == null) return
        for (branch in expression.branches) {
            if (!branch.hasGuard || branch.guardKeywordSource == null) continue
            if (!branch.hasCorrectGuardKeyword) {
                reporter.reportOn(branch.guardKeywordSource, FirErrors.INCORRECT_GUARD_KEYWORD, context)
            }
        }
    }
}