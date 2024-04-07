/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeResolutionResultOverridesOtherToPreserveCompatibility

object FirCustomEnumEntriesMigrationQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    override fun check(expression: FirResolvedQualifier, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.PrioritizedEnumEntries)) return
        if (expression.symbol?.name != StandardNames.ENUM_ENTRIES) return
        if (expression.nonFatalDiagnostics.none { it is ConeResolutionResultOverridesOtherToPreserveCompatibility }) return

        reporter.reportOn(expression.source, FirErrors.DEPRECATED_ACCESS_TO_ENTRIES_AS_QUALIFIER, context)
    }
}
