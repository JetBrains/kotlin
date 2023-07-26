/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.explicitReceiverIsNotSuperReference
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol

object FirSuperCallWithDefaultsChecker : FirFunctionCallChecker() {

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.explicitReceiverIsNotSuperReference()) return

        val functionSymbol = expression.calleeReference.toResolvedNamedFunctionSymbol() ?: return
        if (!functionSymbol.valueParameterSymbols.any { it.hasDefaultValue }) return
        val arguments = expression.argumentList as? FirResolvedArgumentList ?: return
        if (arguments.arguments.size < functionSymbol.valueParameterSymbols.size) {
            reporter.reportOn(
                expression.calleeReference.source,
                FirJvmErrors.SUPER_CALL_WITH_DEFAULT_PARAMETERS,
                functionSymbol.name.asString(),
                context
            )
        }
    }
}
