/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.util.OperatorNameConventions

object FirJsDebugInfoExpressionChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) = when (expression) {
        is FirQualifiedAccessExpression -> check(expression, context, reporter)
        else -> {}
    }

    private fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val calleeDeclaration = expression.calleeReference.resolvedSymbol as? FirCallableSymbol<*>

        // Each increment/decrement involves a pair (GET, SET),
        // but the underlying source is the same.
        val isIncrementOrDecrementReceiver =
            expression.calleeReference.source?.kind == KtFakeSourceElementKind.DesugaredIncrementOrDecrement
                    && calleeDeclaration?.name == OperatorNameConventions.SET

        if (
            calleeDeclaration?.origin is FirDeclarationOrigin.DynamicScope &&
            !isIncrementOrDecrementReceiver &&
            expression.calleeReference.source?.kind != KtFakeSourceElementKind.DesugaredForLoop
        ) {
            reporter.reportOn(expression.calleeReference.source, FirJsErrors.DYNAMIC, context)
        }
    }
}