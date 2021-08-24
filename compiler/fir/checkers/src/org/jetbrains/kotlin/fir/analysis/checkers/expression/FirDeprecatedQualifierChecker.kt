/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDeprecated
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

object FirDeprecatedQualifierChecker : FirResolvedQualifierChecker() {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        expression.nonFatalDiagnostics.filterIsInstance<ConeDeprecated>().forEach { diagnostic ->
            FirDeprecationChecker.reportDeprecation(diagnostic.source, diagnostic.candidateSymbol, diagnostic.deprecation, reporter, context)
        }
        if (expression.resolvedToCompanionObject) {
            val companionSymbol = (expression.symbol as? FirRegularClassSymbol)?.companionObjectSymbol ?: return
            FirDeprecationChecker.reportDeprecationIfNeeded(expression.source, companionSymbol, null, context, reporter)
        }
    }
}