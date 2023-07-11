/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

/**
 * [K1 counterpart checker][org.jetbrains.kotlin.resolve.checkers.NonFinalExpectClassifierHasTheSameMembersAsActualClassifierChecker]
 */
object FirNonFinalExpectClassifierHasTheSameMembersAsActualClassifierChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirRegularClass) return
        if (!declaration.isActual) return
        // We are looking for non-final expect declarations but since non-final expects can be actualized
        // only by actuals that have precisely the same modality, we are interested only in non-final actuals
        if (declaration.isFinal) return
        val actual = declaration.symbol

        val expect = declaration.symbol.expectForActual
            ?.get(ExpectActualCompatibility.Compatible)
            ?.singleOrNull() as? FirRegularClassSymbol
            ?: return

        // todo val session = expect.moduleData.session
        val expectScope = expect.declaredMemberScope(context.session, memberRequiredPhase = null)
        val actualScope = actual.declaredMemberScope(context.session, memberRequiredPhase = null)

        if ((actualScope.getCallableNames() - expectScope.getCallableNames()).isNotEmpty()) {
            reporter.reportOn(declaration.source, FirErrors.EXPECT_AND_ACTUAL_DIFFERENT_MEMBERS, context)
        }
    }
}
