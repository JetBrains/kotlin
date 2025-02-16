/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction

object FirInfixFunctionDeclarationChecker : FirFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.status.isInfix) return
        if (
            (declaration.valueParameters.size != 1) ||
            !hasExtensionOrDispatchReceiver(declaration, context) ||
            declaration.valueParameters.single().isVararg
        ) {
            reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_INFIX_MODIFIER, context)
        }
    }

    private fun hasExtensionOrDispatchReceiver(
        function: FirFunction,
        context: CheckerContext
    ): Boolean {
        if (function.receiverParameter != null) return true
        return context.containingDeclarations.lastOrNull() is FirClass
    }
}
