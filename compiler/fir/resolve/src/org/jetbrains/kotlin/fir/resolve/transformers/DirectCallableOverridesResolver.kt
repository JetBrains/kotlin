/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.addDirectOverrides
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.originalForSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processDirectlyOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.processDirectlyOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol

class DirectCallableOverridesResolver(
    override val session: FirSession,
    override val scopeSession: ScopeSession
) : SessionAndScopeSessionHolder {

    // Do not force type calculation! It is possible that the implicit type body resolution calculates the return type ref
    // of overridden callables as FirImplicitTypeRefImplWithoutSource
    private fun FirCallableSymbol<*>.containingClassScope(implicitBodyResolve: Boolean): FirTypeScope? =
        containingClassLookupTag()?.toClassSymbol()?.unsubstitutedScope(
            withForcedTypeCalculator = !implicitBodyResolve,
            memberRequiredPhase = FirResolvePhase.STATUS
        )

    fun resolveNamedFunction(namedFunction: FirNamedFunction, implicitBodyResolve: Boolean) {
        if (!namedFunction.isOverride) return
        val functionSymbol = namedFunction.symbol
        val containingClassScope = functionSymbol.containingClassScope(implicitBodyResolve) ?: return
        // Prewarm the use-site scope
        containingClassScope.processFunctionsByName(namedFunction.name) {}
        containingClassScope.processDirectlyOverriddenFunctions(functionSymbol) { overriddenSymbol ->
            if (functionSymbol == overriddenSymbol) ProcessorAction.NONE
            if (overriddenSymbol is FirIntersectionCallableSymbol) {
                overriddenSymbol.intersections.forEach { it.fir.addDirectOverrides(functionSymbol) }
            } else {
                (overriddenSymbol.originalForSubstitutionOverride ?: overriddenSymbol).fir.addDirectOverrides(functionSymbol)
            }
            ProcessorAction.NEXT
        }
    }

    fun resolveProperty(property: FirProperty, implicitBodyResolve: Boolean) {
        if (!property.isOverride) return
        val propertySymbol = property.symbol
        val containingClassScope = propertySymbol.containingClassScope(implicitBodyResolve) ?: return
        // Prewarm the use-site scope
        containingClassScope.processPropertiesByName(property.name) {}
        containingClassScope.processDirectlyOverriddenProperties(propertySymbol) { overriddenSymbol ->
            if (propertySymbol == overriddenSymbol) ProcessorAction.NONE
            if (overriddenSymbol is FirIntersectionCallableSymbol) {
                overriddenSymbol.intersections.forEach { it.fir.addDirectOverrides(propertySymbol) }
            } else {
                (overriddenSymbol.originalForSubstitutionOverride ?: overriddenSymbol).fir.addDirectOverrides(propertySymbol)
            }
            ProcessorAction.NEXT
        }
    }
}
