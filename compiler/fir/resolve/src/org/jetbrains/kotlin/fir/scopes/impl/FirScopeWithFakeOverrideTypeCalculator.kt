/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.isIntersectionOverride
import org.jetbrains.kotlin.fir.isSubstitutionOverride
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FakeOverrideTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class FirScopeWithFakeOverrideTypeCalculator(
    private val delegate: FirTypeScope,
    private val fakeOverrideTypeCalculator: FakeOverrideTypeCalculator
) : FirTypeScope() {
    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        delegate.processFunctionsByName(name) {
            updateReturnType(it.fir)
            processor(it)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        delegate.processPropertiesByName(name) {
            updateReturnType(it.fir)
            processor(it)
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    override fun mayContainName(name: Name): Boolean {
        return delegate.mayContainName(name)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return delegate.processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { symbol, scope ->
            updateReturnType(symbol.fir)
            processor(symbol, scope)
        }
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return delegate.processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { symbol, scope ->
            updateReturnType(symbol.fir)
            processor(symbol, scope)
        }
    }

    override fun getCallableNames(): Set<Name> {
        return delegate.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return delegate.getClassifierNames()
    }

    private fun updateReturnType(declaration: FirTypedDeclaration) {
        if (declaration !is FirCallableDeclaration) return
        if (declaration.isSubstitutionOverride || declaration.isIntersectionOverride) {
            fakeOverrideTypeCalculator.computeReturnType(declaration)
        }
    }

    override val scopeOwnerLookupNames: List<String>
        get() = delegate.scopeOwnerLookupNames
}
