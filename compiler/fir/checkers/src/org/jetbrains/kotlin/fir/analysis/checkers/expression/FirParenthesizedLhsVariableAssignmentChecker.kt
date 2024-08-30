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
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.psi.psiUtil.hasUnwrappableAsAssignmentLhs

object FirParenthesizedLhsVariableAssignmentChecker : FirVariableAssignmentChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        // LHS contains an unwrapped expression, and it's easier to walk it up here
        // than to set the proper non-unwrapped source in raw FIR builders.

        // For:
        // - `(x) = ""` where `x: String`
        // - `(x) += ""` where `x: String`
        // - `(getInt()) += 343`
        // - `(a?.w)++` where `a: A?` with `var w: Int` - note: such cases are red code already as `VARIABLE_EXPECTED` or something else.
        val isLhsParenthesized = expression.source.hasUnwrappableAsAssignmentLhs()

        if (isLhsParenthesized) {
            reporter.reportOn(expression.source, FirErrors.WRAPPED_LHS_IN_ASSIGNMENT, context)
        }
    }
}
