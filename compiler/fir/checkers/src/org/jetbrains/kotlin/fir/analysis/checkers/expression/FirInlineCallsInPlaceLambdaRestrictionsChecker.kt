/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol

object FirInlineCallsInPlaceLambdaRestrictionsChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val variableSymbol = expression.calleeReference.toResolvedVariableSymbol() ?: return
        if (!variableSymbol.isVar) return

        val containingLambda = context.containingDeclarations.filterIsInstance<FirAnonymousFunctionSymbol>().lastOrNull() ?: return
        if (!expression.partOfCall()) return
        if (!containingLambda.isLambdaArgumentOfSomeCall()) return

        reporter.reportOn(expression.source, FirErrors.USAGE_IS_NOT_INLINABLE, variableSymbol)
    }

    context(context: CheckerContext)
    private fun FirExpression.partOfCall(): Boolean {
        val callsOrAssignments = context.callsOrAssignments
        val containingQualifiedAccess = callsOrAssignments.getOrNull(callsOrAssignments.size - 2) ?: return false
        if (this == (containingQualifiedAccess as? FirQualifiedAccessExpression)?.explicitReceiver?.unwrapErrorExpression()) return true
        val call = containingQualifiedAccess as? FirCall ?: return false
        val mapping = call.resolvedArgumentMapping ?: return false
        return mapping.keys.any { arg -> arg.unwrapErrorExpression().unwrapArgument() == this }
    }

    context(context: CheckerContext)
    private fun FirAnonymousFunctionSymbol.isLambdaArgumentOfSomeCall(): Boolean {
        for (call in context.callsOrAssignments) {
            val functionCall = call as? FirCall ?: continue
            val mapping = functionCall.resolvedArgumentMapping ?: continue
            for ((argument, _) in mapping) {
                val anonymous = argument.unwrapErrorExpression()
                    .unwrapArgument() as? FirAnonymousFunctionExpression
                val af = anonymous?.anonymousFunction ?: continue
                if (af.symbol === this) return true
            }
        }
        return false
    }
}