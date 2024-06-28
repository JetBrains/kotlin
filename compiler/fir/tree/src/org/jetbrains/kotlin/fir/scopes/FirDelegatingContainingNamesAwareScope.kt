/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

/**
 * A utility [FirContainingNamesAwareScope] which delegates to [delegate] by default for the purpose of reducing boilerplate code.
 * Inheritors must override at least some functions, or else [delegate] could be used directly.
 *
 * [toString] is not delegated by default because the [delegate] is usually not the same kind of scope as this delegating scope.
 */
abstract class FirDelegatingContainingNamesAwareScope(private val delegate: FirContainingNamesAwareScope) : FirContainingNamesAwareScope() {
    override fun getCallableNames(): Set<Name> = delegate.getCallableNames()
    override fun getClassifierNames(): Set<Name> = delegate.getClassifierNames()
    override fun mayContainName(name: Name): Boolean = delegate.mayContainName(name)
    override val scopeOwnerLookupNames: List<String> get() = delegate.scopeOwnerLookupNames

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(
        name: Name,
        processor: (FirNamedFunctionSymbol) -> Unit,
    ) {
        delegate.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(
        name: Name,
        processor: (FirVariableSymbol<*>) -> Unit,
    ) {
        delegate.processPropertiesByName(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }
}

/**
 * A utility [FirTypeScope] which delegates to [delegate] by default for the purpose of reducing boilerplate code. Inheritors must override
 * at least some functions, or else [delegate] could be used directly.
 *
 * [toString] is not delegated by default because the [delegate] is usually not the same kind of scope as this delegating scope.
 */
abstract class FirDelegatingTypeScope(private val delegate: FirTypeScope) : FirTypeScope() {
    override fun getCallableNames(): Set<Name> = delegate.getCallableNames()
    override fun getClassifierNames(): Set<Name> = delegate.getClassifierNames()
    override fun mayContainName(name: Name): Boolean = delegate.mayContainName(name)
    override val scopeOwnerLookupNames: List<String> get() = delegate.scopeOwnerLookupNames

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(
        name: Name,
        processor: (FirNamedFunctionSymbol) -> Unit,
    ) {
        delegate.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(
        name: Name,
        processor: (FirVariableSymbol<*>) -> Unit,
    ) {
        delegate.processPropertiesByName(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        return delegate.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor)
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        return delegate.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor)
    }
}
