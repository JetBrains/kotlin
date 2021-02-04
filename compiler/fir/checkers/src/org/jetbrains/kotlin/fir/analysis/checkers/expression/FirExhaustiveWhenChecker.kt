/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression

object FirExhaustiveWhenChecker : FirWhenExpressionChecker() {
    override fun check(expression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // TODO: add reporting of proper missing clauses, see class WhenMissingCase
        if (expression.usedAsExpression && !expression.isExhaustive) {
            val factory = if (expression.source?.isIfExpression == true) {
                FirErrors.INVALID_IF_AS_EXPRESSION
            } else {
                FirErrors.NO_ELSE_IN_WHEN
            }
            reporter.reportOn(expression.source, factory, context)
        }
    }

    private val FirSourceElement.isIfExpression: Boolean
        get() = elementType == KtNodeTypes.IF
}
