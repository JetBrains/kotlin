/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.js.checkers.checkJsModuleUsage
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol

object FirJsModuleQualifiedAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        checkReifiedTypeParameters(expression)

        val calleeSymbols = extractModuleCalleeSymbols(expression)
        for ((calleeSymbol, source) in calleeSymbols) {
            checkJsModuleUsage(calleeSymbol, source ?: expression.source)
        }
    }

    private fun extractModuleCalleeSymbols(
        expression: FirQualifiedAccessExpression
    ): List<Pair<FirBasedSymbol<*>, AbstractKtSourceElement?>> {
        val calleeSymbol = expression.calleeReference.toResolvedBaseSymbol()
        if (calleeSymbol != null && calleeSymbol.getContainingClassSymbol() == null) {
            return listOf(calleeSymbol to expression.calleeReference.source)
        }

        return when (val receiver = expression.dispatchReceiver?.unwrapSmartcastExpression()) {
            null -> listOfNotNull(calleeSymbol?.to(expression.calleeReference.source))
            is FirResolvedQualifier -> {
                val classSymbol = receiver.symbol
                if (expression is FirCallableReferenceAccess) {
                    listOfNotNull(classSymbol?.to(receiver.source), calleeSymbol?.to(expression.calleeReference.source))
                } else {
                    listOfNotNull(classSymbol?.to(expression.calleeReference.source))
                }
            }
            else -> emptyList()
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkReifiedTypeParameters(
        expr: FirQualifiedAccessExpression,
    ) {
        (expr as? FirFunctionCall)?.forAllReifiedTypeParameters { type, typeArgument ->
            val typeArgumentClass = type.toRegularClassSymbol() ?: return@forAllReifiedTypeParameters
            val source = typeArgument.source ?: expr.calleeReference.source ?: expr.source
            checkJsModuleUsage(typeArgumentClass, source)
        }
    }
}


