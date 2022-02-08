/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier

object FirOptInUsageQualifierChecker : FirResolvedQualifierChecker() {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.symbol ?: return
        with(FirOptInUsageBaseChecker) {
            val experimentalities = symbol.loadExperimentalities(context, fromSetter = false, dispatchReceiverType = null)
            reportNotAcceptedExperimentalities(experimentalities, expression, context, reporter)
        }
    }
}