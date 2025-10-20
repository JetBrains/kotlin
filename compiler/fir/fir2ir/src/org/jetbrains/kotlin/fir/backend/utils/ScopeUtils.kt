/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.resolve.isRealOwnerOf
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.types.ConeIntersectionType

context(c: Fir2IrComponents)
internal tailrec fun FirCallableSymbol<*>.unwrapCallRepresentative(
    owner: ConeClassLikeLookupTag? = containingClassLookupTag()
): FirCallableSymbol<*> {
    val fir = fir

    if (fir is FirConstructor) {
        val originalForTypeAlias = fir.typeAliasConstructorInfo?.originalConstructor
        if (originalForTypeAlias != null) {
            return originalForTypeAlias.symbol.unwrapCallRepresentative(owner)
        }
    }

    if (fir.isIntersectionOverride) {
        // We've got IR declarations (fake overrides) for intersection overrides in classes, but not for intersection types
        // interface A { fun foo() }
        // interface B { fun foo() }
        // interface C : A, B // for C.foo we've got an IR fake override
        // for {A & B} we don't have such an IR declaration, so we're unwrapping it
        if (fir.dispatchReceiverType is ConeIntersectionType) {
            return fir.baseForIntersectionOverride!!.symbol.unwrapCallRepresentative(owner)
        }

        return this
    }

    val originalForOverride = fir.originalForSubstitutionOverride
    if (originalForOverride != null && originalForOverride.containingClassLookupTag() == owner) {
        return originalForOverride.symbol.unwrapCallRepresentative(owner)
    }

    return this
}

context(c: Fir2IrComponents)
internal fun FirNamedFunction.processOverriddenFunctionSymbols(
    containingClass: FirClass,
    processor: (FirNamedFunctionSymbol) -> Unit
) {
    val scope = containingClass.unsubstitutedScope()
    scope.processFunctionsByName(name) {}
    scope.processOverriddenFunctionsFromSuperClasses(symbol, containingClass) { overriddenSymbol ->
        if (!c.session.visibilityChecker.isVisibleForOverriding(
                candidateInDerivedClass = symbol.fir, candidateInBaseClass = overriddenSymbol.fir
            )
        ) {
            return@processOverriddenFunctionsFromSuperClasses ProcessorAction.NEXT
        }
        processor(overriddenSymbol)

        ProcessorAction.NEXT
    }
}

fun FirTypeScope.processOverriddenFunctionsFromSuperClasses(
    functionSymbol: FirNamedFunctionSymbol,
    containingClass: FirClass,
    processor: (FirNamedFunctionSymbol) -> ProcessorAction
): ProcessorAction {
    val ownerTag = containingClass.symbol.toLookupTag()
    return processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { overridden, _ ->
        val unwrapped = if (overridden.fir.isSubstitutionOverride && ownerTag.isRealOwnerOf(overridden))
            overridden.originalForSubstitutionOverride!!
        else
            overridden

        processor(unwrapped)
    }
}

fun FirTypeScope.processOverriddenPropertiesFromSuperClasses(
    propertySymbol: FirPropertySymbol,
    containingClass: FirClass,
    processor: (FirPropertySymbol) -> ProcessorAction
): ProcessorAction {
    val ownerTag = containingClass.symbol.toLookupTag()
    return processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { overridden, _ ->
        val unwrapped = if (overridden.fir.isSubstitutionOverride && ownerTag.isRealOwnerOf(overridden))
            overridden.originalForSubstitutionOverride!!
        else
            overridden

        processor(unwrapped)
    }
}

context(c: Fir2IrComponents)
internal fun FirProperty.processOverriddenPropertySymbols(
    containingClass: FirClass,
    processor: (FirPropertySymbol) -> Unit
) {
    val scope = containingClass.unsubstitutedScope()
    scope.processPropertiesByName(name) {}
    scope.processOverriddenPropertiesFromSuperClasses(symbol, containingClass) { overriddenSymbol ->
        if (!c.session.visibilityChecker.isVisibleForOverriding(
                candidateInDerivedClass = symbol.fir, candidateInBaseClass = overriddenSymbol.fir
            )
        ) {
            return@processOverriddenPropertiesFromSuperClasses ProcessorAction.NEXT
        }
        processor(overriddenSymbol)

        ProcessorAction.NEXT
    }
}

context(c: Fir2IrComponents)
internal fun FirClassSymbol<*>.unsubstitutedScope(): FirTypeScope {
    return this.unsubstitutedScope(c.session, c.scopeSession, withForcedTypeCalculator = true, memberRequiredPhase = FirResolvePhase.STATUS)
}

context(c: Fir2IrComponents)
internal fun FirClass.unsubstitutedScope(): FirTypeScope {
    return symbol.unsubstitutedScope()
}

context(c: Fir2IrComponents)
internal fun FirClassSymbol<*>.declaredScope(): FirContainingNamesAwareScope {
    return this.declaredMemberScope(c.session, memberRequiredPhase = FirResolvePhase.STATUS)
}
