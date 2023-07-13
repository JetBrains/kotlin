/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.js.checkers.checkJsModuleUsage
import org.jetbrains.kotlin.fir.analysis.js.checkers.isTopLevelSymbol
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirJsModuleQualifiedAccessChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        checkReifiedTypeParameters(expression, context, reporter)

        val calleeSymbols = extractModuleCalleeSymbols(expression)
        for ((calleeSymbol, source) in calleeSymbols) {
            checkJsModuleUsage(calleeSymbol, context, reporter, source ?: expression.source)
        }
    }

    private fun extractModuleCalleeSymbols(
        expression: FirQualifiedAccessExpression
    ): List<Pair<FirBasedSymbol<*>, AbstractKtSourceElement?>> {
        val calleeSymbol = expression.calleeReference.toResolvedBaseSymbol()
        if (calleeSymbol != null && isTopLevelSymbol(calleeSymbol, calleeSymbol.moduleData.session)) {
            return listOf(calleeSymbol to expression.calleeReference.source)
        }

        return when (val receiver = expression.dispatchReceiver) {
            is FirNoReceiverExpression -> listOfNotNull(calleeSymbol?.to(expression.calleeReference.source))
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

    private fun checkReifiedTypeParameters(expr: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val functionSymbol = (expr as? FirFunctionCall)?.calleeReference?.toResolvedFunctionSymbol() ?: return
        for ((typeParameterSymbol, typeArgument) in functionSymbol.typeParameterSymbols.zip(expr.typeArguments)) {
            if (typeParameterSymbol.isReified) {
                val type = (typeArgument as? FirTypeProjectionWithVariance)?.typeRef?.coneTypeOrNull ?: continue
                val typeArgumentClass = type.toRegularClassSymbol(context.session) ?: continue
                val source = typeArgument.source ?: expr.calleeReference.source ?: expr.source
                checkJsModuleUsage(typeArgumentClass, context, reporter, source)
            }
        }
    }
}


