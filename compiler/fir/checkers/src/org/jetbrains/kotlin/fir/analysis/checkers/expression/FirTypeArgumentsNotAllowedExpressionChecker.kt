/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier

object FirTypeArgumentsNotAllowedExpressionChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // analyze type parameters near
        // package names
        val explicitReceiver = expression.explicitReceiver

        if (explicitReceiver is FirResolvedQualifier && explicitReceiver.symbol == null) {
            if (explicitReceiver.typeArguments.isNotEmpty()) {
                reporter.reportOn(explicitReceiver.source, FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED, context)
                return
            }
        }
    }
}
