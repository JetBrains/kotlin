/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction

object FirAnonymousFunctionParametersChecker : FirAnonymousFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirAnonymousFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        for (valueParameter in declaration.valueParameters) {
            val source = valueParameter.source ?: continue
            if (valueParameter.defaultValue != null) {
                reporter.reportOn(source, FirErrors.ANONYMOUS_FUNCTION_PARAMETER_WITH_DEFAULT_VALUE, context)
            }
            if (valueParameter.isVararg) {
                reporter.reportOn(source, FirErrors.USELESS_VARARG_ON_PARAMETER, context)
            }
        }
    }
}
