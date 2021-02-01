/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extended.report
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirStatement

object FirAnonymousFunctionChecker : FirExpressionChecker<FirStatement>() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirAnonymousFunction) {
            return
        }
        for (valueParameter in expression.valueParameters) {
            val source = valueParameter.source ?: continue
            if (valueParameter.defaultValue != null) {
                reporter.report(source, FirErrors.ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE)
            }
            if (valueParameter.isVararg) {
                reporter.report(source, FirErrors.USELESS_VARARG_ON_PARAMETER)
            }
        }
    }
}
