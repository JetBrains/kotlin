/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
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
import org.jetbrains.kotlin.fir.symbols.impl.FirIntersectionCallableSymbol

class DirectCallableOverridesResolver(
    override val session: FirSession,
    override val scopeSession: ScopeSession
) : SessionAndScopeSessionHolder {

    private fun FirCallableDeclaration.containingClassScope(): FirTypeScope? {
        val containingClass = containingClassLookupTag()?.toClassSymbol() ?: return null
        return containingClass.unsubstitutedScope(withForcedTypeCalculator = true, memberRequiredPhase = FirResolvePhase.STATUS)
    }

    fun resolveNamedFunction(namedFunction: FirNamedFunction) {
        if (!namedFunction.isOverride) return
        val containingClassScope = namedFunction.containingClassScope() ?: return
        val functionSymbol = namedFunction.symbol
        // Prewarm the use-site scope
        containingClassScope.processFunctionsByName(namedFunction.name) {}
        containingClassScope.processDirectlyOverriddenFunctions(functionSymbol) { overriddenSymbol ->
            if (functionSymbol == overriddenSymbol) ProcessorAction.NONE
            else if (overriddenSymbol is FirIntersectionCallableSymbol) {
                overriddenSymbol.intersections.forEach { it.fir.addDirectOverrides(functionSymbol) }
            } else {
                (overriddenSymbol.originalForSubstitutionOverride ?: overriddenSymbol).fir.addDirectOverrides(functionSymbol)
            }
            ProcessorAction.NEXT
        }
    }

    fun resolveProperty(property: FirProperty) {
        if (!property.isOverride) return
        val containingClassScope = property.containingClassScope() ?: return
        val propertySymbol = property.symbol
        // Prewarm the use-site scope
        containingClassScope.processPropertiesByName(property.name) {}
        containingClassScope.processDirectlyOverriddenProperties(propertySymbol) { overriddenSymbol ->
            if (propertySymbol == overriddenSymbol) ProcessorAction.NONE
            else if (overriddenSymbol is FirIntersectionCallableSymbol) {
                overriddenSymbol.intersections.forEach { it.fir.addDirectOverrides(propertySymbol) }
            } else {
                (overriddenSymbol.originalForSubstitutionOverride ?: overriddenSymbol).fir.addDirectOverrides(propertySymbol)
            }
            ProcessorAction.NEXT
        }
    }
}
