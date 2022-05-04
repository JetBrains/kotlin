/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessChecker
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.languageVersionSettings

object FirDirectFieldOrDelegateAccessUnsupportedChecker : FirQualifiedAccessChecker() {
    override fun check(expression: FirQualifiedAccess, context: CheckerContext, reporter: DiagnosticReporter) {
        val isAllowed = context.session.languageVersionSettings.supportsFeature(LanguageFeature.DirectFieldOrDelegateAccess)

        if (expression.searchSynthetics && !isAllowed) {
            reporter.reportOn(
                expression.source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.DirectFieldOrDelegateAccess to context.session.languageVersionSettings,
                context
            )
        }
    }
}
