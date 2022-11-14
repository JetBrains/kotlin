/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.Name

private val fqNames = listOf(
    StandardNames.FqNames.charSequence.child(Name.identifier("get")),
    StandardNames.FqNames.charIterator.toUnsafe().child(Name.identifier("nextChar")),
)

private val simpleNames = fqNames.mapTo(mutableSetOf()) { it.shortName() }

object FirJsMultipleInheritanceChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val scope = declaration.unsubstitutedScope(context)

        val members = mutableListOf<FirNamedFunctionSymbol>().apply {
            for (it in simpleNames) {
                scope.processFunctionsByName(it, this::add)
            }
        }

        for (callable in members) {
            val overridden = callable.overriddenFunctions(declaration.symbol, context)

            if (
                overridden.size > 1 &&
                overridden.any { it.callableId.asSingleFqName().toUnsafe() in fqNames }
            ) {
                reporter.reportOn(declaration.source, FirJsErrors.WRONG_MULTIPLE_INHERITANCE, callable, context)
            }
        }
    }
}