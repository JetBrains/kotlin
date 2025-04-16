/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol

object FirInlineBodyResolvableExpressionChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        val inlineFunctionBodyContext = context.inlineFunctionBodyContext ?: return
        if (expression !is FirQualifiedAccessExpression && expression !is FirDelegatedConstructorCall) return
        val targetSymbol = expression.toResolvedCallableSymbol()

        inlineFunctionBodyContext.checkQualifiedAccess(expression, targetSymbol)

        if (expression is FirQualifiedAccessExpression) {
            inlineFunctionBodyContext.checkReceiversOfQualifiedAccessExpression(expression, targetSymbol)
        }

        if (expression is FirFunctionCall) {
            inlineFunctionBodyContext.checkArgumentsOfCall(expression, targetSymbol)
        }
    }
}
