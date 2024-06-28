/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.languageVersionSettings

object FirExplicitBackingFieldsUnsupportedChecker : FirBackingFieldChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirBackingField, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirDefaultPropertyBackingField &&
            !context.session.languageVersionSettings.supportsFeature(LanguageFeature.ExplicitBackingFields)
        ) {
            reporter.reportOn(
                declaration.source,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.ExplicitBackingFields to context.session.languageVersionSettings,
                context
            )
        }
    }
}
