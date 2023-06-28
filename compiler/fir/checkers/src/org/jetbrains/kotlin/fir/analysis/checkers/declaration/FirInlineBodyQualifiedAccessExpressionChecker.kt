/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol

object FirInlineBodyQualifiedAccessExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val inlineFunctionBodyContext = context.inlineFunctionBodyContext ?: return
        val targetSymbol = expression.toResolvedCallableSymbol()

        inlineFunctionBodyContext.checkQualifiedAccess(expression, targetSymbol, context, reporter)
        inlineFunctionBodyContext.checkReceiversOfQualifiedAccessExpression(expression, targetSymbol, context, reporter)

        if (expression is FirFunctionCall) {
            inlineFunctionBodyContext.checkArgumentsOfCall(expression, targetSymbol, context, reporter)
        }
    }
}
