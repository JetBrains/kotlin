/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.declarations.utils.isErrorPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object FirCommonConstructorDelegationIssuesChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingClass = context.containingDeclarations.lastIsInstanceOrNull<FirRegularClass>()
        if (declaration.isEffectivelyExternal(containingClass, context)) return
        val cyclicConstructors = mutableSetOf<FirConstructorSymbol>()
        var hasPrimaryConstructor = false
        val isEffectivelyExpect = declaration.isEffectivelyExpect(context.containingDeclarations.lastOrNull() as? FirRegularClass, context)

        // secondary; non-cyclic;
        // candidates for further analysis
        val otherConstructors = mutableSetOf<FirConstructorSymbol>()

        declaration.constructors(context.session).forEach {
            if (!it.isPrimary || it.isErrorPrimaryConstructor) {
                otherConstructors += it

                it.findCycle(cyclicConstructors)?.let { visited ->
                    cyclicConstructors += visited
                }
            } else {
                hasPrimaryConstructor = true
            }
        }

        otherConstructors -= cyclicConstructors

        if (hasPrimaryConstructor) {
            for (it in otherConstructors) {
                if (!isEffectivelyExpect && it.resolvedDelegatedConstructorCall?.isThis != true) {
                    reporter.reportOn(
                        it.resolvedDelegatedConstructorCall?.source ?: it.source,
                        FirErrors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED,
                        context
                    )
                }
            }
        } else {
            for (it in otherConstructors) {
                // couldn't find proper super() constructor implicitly
                if (it.resolvedDelegatedConstructorCall?.calleeReference is FirDiagnosticHolder &&
                    it.resolvedDelegatedConstructorCall?.source?.kind is KtFakeSourceElementKind &&
                    !it.isExpect
                ) {
                    reporter.reportOn(it.source, FirErrors.EXPLICIT_DELEGATION_CALL_REQUIRED, context)
                }
            }
        }

        cyclicConstructors.forEach {
            reporter.reportOn(it.resolvedDelegatedConstructorCall?.source, FirErrors.CYCLIC_CONSTRUCTOR_DELEGATION_CALL, context)
        }
    }

    private fun FirConstructorSymbol.findCycle(knownCyclicConstructors: Set<FirConstructorSymbol> = emptySet()): Set<FirConstructorSymbol>? {
        val visitedConstructors = mutableSetOf(this)

        var it = this
        var delegated = this.getDelegated()

        while (!(it.isPrimary && !it.isErrorPrimaryConstructor) && delegated != null) {
            if (delegated in visitedConstructors || delegated in knownCyclicConstructors) {
                return visitedConstructors
            }

            it = delegated
            delegated = delegated.getDelegated()
            visitedConstructors.add(it)
        }

        return null
    }

    @OptIn(SymbolInternals::class)
    private fun FirConstructorSymbol.getDelegated(): FirConstructorSymbol? {
        this.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return fir.delegatedConstructor?.calleeReference?.toResolvedConstructorSymbol(discardErrorReference = true)
    }
}
