/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.analysis.checkers.FirDeclarationInspector
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

object FirConflictsChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val inspector = FirDeclarationInspector()

        when (declaration) {
            is FirFile -> checkFile(declaration, inspector)
            is FirRegularClass -> checkRegularClass(declaration, inspector)
            else -> return
        }

        inspector.functionDeclarations.forEachNonSingle { it, symbols ->
            reporter.reportOn(it.source, FirErrors.CONFLICTING_OVERLOADS, symbols, context)
        }

        inspector.otherDeclarations.forEachNonSingle { it, symbols ->
            reporter.reportOn(it.source, FirErrors.REDECLARATION, symbols, context)
        }
    }

    private fun Map<String, List<FirDeclaration>>.forEachNonSingle(action: (FirDeclaration, Collection<AbstractFirBasedSymbol<*>>) -> Unit) {
        for (value in values) {
            if (value.size > 1) {
                val symbols = value.mapNotNull { (it as? FirSymbolOwner<*>)?.symbol }

                value.forEach {
                    action(it, symbols)
                }
            }
        }
    }

    private fun checkFile(declaration: FirFile, inspector: FirDeclarationInspector) {
        for (it in declaration.declarations) {
            inspector.collect(it)
        }
    }

    private fun checkRegularClass(declaration: FirRegularClass, inspector: FirDeclarationInspector) {
        for (it in declaration.declarations) {
            inspector.collect(it)
        }
    }
}
