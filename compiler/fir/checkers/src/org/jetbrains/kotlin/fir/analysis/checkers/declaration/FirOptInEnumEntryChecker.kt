/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.loadExperimentalitiesFromConstructor
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.reportNotAcceptedExperimentalities
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.getPrimaryConstructorSymbol
import org.jetbrains.kotlin.fir.isEnabled

object FirOptInEnumEntryChecker : FirEnumEntryChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirEnumEntry) {
        if (declaration.initializer != null) return
        val primaryConstructorSymbol = declaration.symbol.getContainingClassSymbol()?.getPrimaryConstructorSymbol(
            context.session, context.scopeSession
        ) ?: return
        val experimentalities = primaryConstructorSymbol.loadExperimentalitiesFromConstructor().map {
            if (LanguageFeature.CheckOptInOnPureEnumEntries.isEnabled()) it
            else it.copy(severity = FirOptInUsageBaseChecker.Experimentality.Severity.WARNING)
        }
        reportNotAcceptedExperimentalities(experimentalities, declaration)
    }
}
