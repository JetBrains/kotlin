/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase

object FirCommonConstructorDelegationIssuesChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val cyclicConstructors = mutableSetOf<FirConstructor>()
        var hasPrimaryConstructor = false
        val isEffectivelyExpect = declaration.isEffectivelyExpect(context.containingDeclarations.lastOrNull() as? FirRegularClass, context)

        // secondary; non-cyclic;
        // candidates for further analysis
        val otherConstructors = mutableSetOf<FirConstructor>()

        for (it in declaration.declarations) {
            if (it is FirConstructor) {
                if (!it.isPrimary) {
                    otherConstructors += it

                    it.findCycle(cyclicConstructors)?.let { visited ->
                        cyclicConstructors += visited
                    }
                } else {
                    hasPrimaryConstructor = true
                }
            }
        }

        otherConstructors -= cyclicConstructors

        if (hasPrimaryConstructor) {
            for (it in otherConstructors) {
                if (!isEffectivelyExpect && it.delegatedConstructor?.isThis != true) {
                    if (it.delegatedConstructor?.source != null) {
                        reporter.reportOn(it.delegatedConstructor?.source, FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED, context)
                    } else {
                        reporter.reportOn(it.source, FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED, context)
                    }
                }
            }
        } else {
            for (it in otherConstructors) {
                val callee = it.delegatedConstructor?.calleeReference

                // couldn't find proper super() constructor implicitly
                if (
                    callee is FirErrorNamedReference && callee.diagnostic is ConeAmbiguityError &&
                    it.delegatedConstructor?.source?.kind is KtFakeSourceElementKind
                ) {
                    reporter.reportOn(it.source, FirErrors.EXPLICIT_DELEGATION_CALL_REQUIRED, context)
                }
            }
        }

        cyclicConstructors.forEach {
            reporter.reportOn(it.delegatedConstructor?.source, FirErrors.CYCLIC_CONSTRUCTOR_DELEGATION_CALL, context)
        }
    }

    private fun FirConstructor.findCycle(knownCyclicConstructors: Set<FirConstructor> = emptySet()): Set<FirConstructor>? {
        val visitedConstructors = mutableSetOf(this)

        var it = this
        var delegated = this.getDelegated()

        while (!it.isPrimary && delegated != null) {
            if (delegated in visitedConstructors || delegated in knownCyclicConstructors) {
                return visitedConstructors
            }

            it = delegated
            delegated = delegated.getDelegated()
            visitedConstructors.add(it)
        }

        return null
    }

    private fun FirConstructor.getDelegated(): FirConstructor? {
        this.symbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        val delegatedConstructorSymbol = (delegatedConstructor?.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
        @OptIn(SymbolInternals::class)
        return delegatedConstructorSymbol?.fir as? FirConstructor
    }
}
