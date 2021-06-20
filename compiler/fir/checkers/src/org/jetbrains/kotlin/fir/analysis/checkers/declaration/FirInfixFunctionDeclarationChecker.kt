/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*

object FirInfixFunctionDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        if ((declaration as? FirStatusOwner)?.status?.isInfix != true) return
        if (declaration is FirSimpleFunction) {
            if (declaration.valueParameters.size != 1 || !hasExtensionOrDispatchReceiver(declaration, context)) {
                reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_INFIX_MODIFIER, context)
            }
            return
        }
        reporter.reportOn(declaration.source, FirErrors.INAPPLICABLE_INFIX_MODIFIER, context)
    }

    private fun hasExtensionOrDispatchReceiver(
        function: FirSimpleFunction,
        context: CheckerContext
    ): Boolean {
        if (function.receiverTypeRef != null) return true
        return context.containingDeclarations.lastOrNull() is FirClass<*>
    }
}
