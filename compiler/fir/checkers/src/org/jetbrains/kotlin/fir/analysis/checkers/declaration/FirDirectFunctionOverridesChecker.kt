/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenFunctionsSafe
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.directOverrides
import org.jetbrains.kotlin.fir.declarations.utils.isOverride

object FirDirectFunctionOverridesChecker : FirNamedFunctionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        if (declaration.source?.kind is KtFakeSourceElementKind) return
        if (!declaration.isOverride) return
        val symbol = declaration.symbol
        for (overriddenSymbol in symbol.directOverriddenFunctionsSafe()) {
            if (symbol !in overriddenSymbol.directOverrides) {
                reporter.reportOn(declaration.source, FirErrors.MISSING_OVERRIDE, overriddenSymbol, symbol)
            }
        }
    }
}
