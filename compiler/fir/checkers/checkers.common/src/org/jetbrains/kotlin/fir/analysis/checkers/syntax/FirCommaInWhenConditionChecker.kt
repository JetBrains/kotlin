/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirWhenExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds

object FirCommaInWhenConditionChecker : FirWhenExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        if (expression.source == null || expression.subjectVariable != null) return

        for (branch in expression.branches) {
            if (branch.condition is FirElseIfTrueCondition) continue
            checkCommaInBranchCondition(branch)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommaInBranchCondition(branch: FirWhenBranch) {
        val source = branch.source
        if (source?.syntaxId == KotlinSyntaxNodeTypeIds.WHEN_ENTRY && withSyntax { source.getChild(KotlinSyntaxTokenIds.COMMA, depth = 1) } != null) {
            reporter.reportOn(source, FirErrors.COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT)
        }
    }
}
