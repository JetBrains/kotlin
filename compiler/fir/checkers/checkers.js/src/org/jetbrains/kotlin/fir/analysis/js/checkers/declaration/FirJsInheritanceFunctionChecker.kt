/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isEffectivelyExternal
import org.jetbrains.kotlin.fir.analysis.js.checkers.isOverridingExternalWithOptionalParams
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

sealed class FirJsInheritanceFunctionChecker(mppKind: MppCheckerKind) : FirFunctionChecker(mppKind) {
    object Regular : FirJsInheritanceFunctionChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirFunction) {
            if ((context.containingDeclarations.last() as? FirClassSymbol<*>)?.isExpect == true) return
            super.check(declaration)
        }
    }

    object ForExpectClass : FirJsInheritanceFunctionChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirFunction) {
            if ((context.containingDeclarations.last() as? FirClassSymbol<*>)?.isExpect != true) return
            super.check(declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (declaration.isNotEffectivelyExternalFunctionButOverridesExternal()) {
            reporter.reportOn(declaration.source, FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS)
        }
    }

    context(context: CheckerContext)
    private fun FirDeclaration.isNotEffectivelyExternalFunctionButOverridesExternal(): Boolean {
        if (this !is FirFunction || symbol.isEffectivelyExternal()) return false
        return symbol.isOverridingExternalWithOptionalParams()
    }
}
