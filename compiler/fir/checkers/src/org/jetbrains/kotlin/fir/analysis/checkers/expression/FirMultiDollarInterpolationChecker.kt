/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.languageVersionSettings

object FirMultiDollarInterpolationChecker : FirStringConcatenationCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirStringConcatenationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        // no interpolation prefix => always OK
        if (expression.interpolationPrefix.isEmpty()) return

        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiDollarInterpolation)) {
            reporter.reportOn(
                expression.source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.MultiDollarInterpolation to context.session.languageVersionSettings,
                context
            )
        } else if (expression.interpolationPrefix.length == 1) {
            reporter.reportOn(expression.source, FirErrors.SINGLE_DOLLAR_INTERPOLATION_PREFIX, context)
        }
    }
}