/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol

object FirTypeArgumentsNotAllowedExpressionChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // analyze type parameters near
        // package names
        val explicitReceiver = expression.explicitReceiver
        if (explicitReceiver is FirResolvedQualifier && explicitReceiver.symbol == null && explicitReceiver.typeArguments.isNotEmpty()) {
            reporter.reportOn(explicitReceiver.source, FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED, "for packages", context)
            return
        }

        if (
            expression is FirImplicitInvokeCall && explicitReceiver is FirPropertyAccessExpression &&
            expression.typeArguments.any { it.source != null } &&
            expression.toResolvedCallableSymbol()?.typeParameterSymbols?.isNotEmpty() == true &&
            explicitReceiver.toResolvedCallableSymbol()?.typeParameterSymbols?.isNotEmpty() == true
        ) {
            reporter.reportOn(expression.calleeReference.source, FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED, "on implicit invoke call", context)
            return
        }
    }
}
