/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration.crv

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirCallableDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenSymbolsSafe
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.resolve.ReturnValueStatus

object FirReturnValueOverrideChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        if (context.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) == ReturnValueCheckerMode.DISABLED) return

        // Only check effectively-mustUse overrides:
        if (!declaration.isOverride) return
        if (declaration.status.returnValueStatus != ReturnValueStatus.MustUse) return
        if (declaration.returnTypeRef.coneType.isIgnorable()) return
        val symbol = declaration.symbol

        // Check if any of the overridden symbols have @IgnorableReturnValue
        val overriddenSymbols = symbol.directOverriddenSymbolsSafe()
        val ignorableBaseSymbol = overriddenSymbols.find {
            it.resolvedStatus.returnValueStatus == ReturnValueStatus.ExplicitlyIgnorable
        } ?: return

        // Report error if an overridden symbol has @IgnorableReturnValue but the current declaration doesn't
        val containingClass = ignorableBaseSymbol.getContainingClassSymbol()
            ?: error("Overridden symbol ${ignorableBaseSymbol.callableId} does not have containing class symbol")
        reporter.reportOn(
            declaration.source,
            FirErrors.OVERRIDING_IGNORABLE_WITH_MUST_USE,
            symbol,
            containingClass,
        )
    }
}
