/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenPropertiesSafe
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.directOverrides
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm

object FirDirectPropertyOverridesChecker : FirPropertyChecker(MppCheckerKind.Common) {

    @OptIn(ScopeFunctionRequiresPrewarm::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        if (LanguageFeature.DirectClassInheritors.isDisabled()) return
        if (declaration.source?.kind is KtFakeSourceElementKind) return
        if (!declaration.isOverride) return
        val symbol = declaration.symbol
        for (overriddenSymbol in symbol.directOverriddenPropertiesSafe()) {
            if (symbol !in overriddenSymbol.directOverrides) {
                reporter.reportOn(declaration.source, FirErrors.MISSING_OVERRIDE, overriddenSymbol, symbol)
            }
        }
    }
}
