/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.lexer.KtTokens

object FirCommaInWhenConditionChecker : FirExpressionSyntaxChecker<FirWhenExpression, PsiElement>() {
    override fun isApplicable(element: FirWhenExpression, source: FirSourceElement): Boolean {
        return element.subject == null
    }

    override fun checkLightTree(
        element: FirWhenExpression,
        source: FirSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (branch in element.branches) {
            if (branch.condition is FirElseIfTrueCondition) continue
            checkCommaInBranchCondition(branch, context, reporter)
        }
    }

    private fun checkCommaInBranchCondition(branch: FirWhenBranch, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = branch.source
        if (source?.elementType == KtNodeTypes.WHEN_ENTRY && source?.getChild(KtTokens.COMMA, depth = 1) != null) {
            reporter.reportOn(source, FirErrors.COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT, context)
        }
    }
}