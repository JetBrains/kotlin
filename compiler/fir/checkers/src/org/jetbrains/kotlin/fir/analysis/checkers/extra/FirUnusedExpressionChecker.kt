/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirUnusedCheckerBase
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.hasSideEffect
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*

object FirUnusedExpressionChecker : FirUnusedCheckerBase() {
    context(context: CheckerContext)
    override fun isEnabled(): Boolean = true // Controlled by FIR_EXTRA_CHECKERS

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun createVisitor(): UsageVisitorBase = UsageVisitor(context, reporter)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportUnused(
        expression: FirExpression,
        source: KtSourceElement?,
    ) {
        val isLambda = expression is FirAnonymousFunctionExpression && expression.anonymousFunction.isLambda
        val factory = when {
            isLambda
                -> FirErrors.UNUSED_LAMBDA_EXPRESSION
            else -> FirErrors.UNUSED_EXPRESSION
        }
        reporter.reportOn(source, factory)
    }

    private class UsageVisitor(context: CheckerContext, reporter: DiagnosticReporter) : UsageVisitorBase(context, reporter) {
        override fun checkExpression(
            expression: FirExpression,
            data: UsageState,
        ) {
            if (!data.isUnused()) return
            if (expression.hasSideEffect()) return
            context(context, reporter) {
                reportUnused(expression, expression.source)
            }
        }
    }
}
