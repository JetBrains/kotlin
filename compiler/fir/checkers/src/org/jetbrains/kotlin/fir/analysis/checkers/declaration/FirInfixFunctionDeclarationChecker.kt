/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.onSource
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.isInfix

object FirInfixFunctionDeclarationChecker : FirDeclarationChecker<FirMemberDeclaration>() {
    override fun check(declaration: FirMemberDeclaration, reporter: DiagnosticReporter) {
        if (declaration is FirSimpleFunction && declaration.isInfix) {
            if (declaration.valueParameters.size != 1 || declaration.receiverTypeRef == null) {
                reporter.report(declaration.source)
            }
            return
        }
        if (declaration.isInfix) {
            reporter.report(declaration.source)
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let { report(Errors.INAPPLICABLE_INFIX_MODIFIER.onSource(it, "Inapplicable infix modifier")) }
    }
}