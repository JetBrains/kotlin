/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.ensureResolved

object FirOptInUsageQualifierChecker : FirResolvedQualifierChecker() {
    @OptIn(SymbolInternals::class)
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.symbol ?: return
        symbol.ensureResolved(FirResolvePhase.STATUS)
        val fir = symbol.fir
        with(FirOptInUsageBaseChecker) {
            val experimentalities = fir.loadExperimentalities(context, fromSetter = false)
            reportNotAcceptedExperimentalities(experimentalities, expression, context, reporter)
        }
    }
}