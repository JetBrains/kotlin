/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

abstract class AbstractFirUseSiteMemberScope(
    session: FirSession,
    overrideChecker: FirOverrideChecker,
    protected val superTypesScope: FirTypeScope,
    protected val declaredMemberScope: FirScope
) : AbstractFirOverrideScope(session, overrideChecker) {

    private val functions = hashMapOf<Name, Collection<FirFunctionSymbol<*>>>()
    private val directOverriddenFunctions = hashMapOf<FirNamedFunctionSymbol, Collection<FirNamedFunctionSymbol>>()
    protected val directOverriddenProperties = hashMapOf<FirPropertySymbol, MutableList<FirPropertySymbol>>()

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        functions.getOrPut(name) {
            doProcessFunctions(name)
        }.forEach {
            processor(it)
        }
    }

    private fun doProcessFunctions(
        name: Name
    ): Collection<FirFunctionSymbol<*>> = mutableListOf<FirFunctionSymbol<*>>().apply {
        val overrideCandidates = mutableSetOf<FirFunctionSymbol<*>>()
        declaredMemberScope.processFunctionsByName(name) { symbol ->
            if (symbol.isStatic) return@processFunctionsByName
            if (symbol is FirNamedFunctionSymbol) {
                val directOverridden = computeDirectOverridden(symbol)
                this@AbstractFirUseSiteMemberScope.directOverriddenFunctions[symbol] = directOverridden
            }
            overrideCandidates += symbol
            add(symbol)
        }

        superTypesScope.processFunctionsByName(name) {
            if (it !is FirConstructorSymbol) {
                val overriddenBy = it.getOverridden(overrideCandidates)
                if (overriddenBy == null) {
                    add(it)
                }
            }
        }
    }

    private fun computeDirectOverridden(symbol: FirNamedFunctionSymbol): Collection<FirNamedFunctionSymbol> {
        val result = mutableListOf<FirNamedFunctionSymbol>()
        val firSimpleFunction = symbol.fir
        superTypesScope.processFunctionsByName(symbol.callableId.callableName) { superSymbol ->
            if (superSymbol is FirNamedFunctionSymbol &&
                overrideChecker.isOverriddenFunction(firSimpleFunction, superSymbol.fir)
            ) {
                result.add(superSymbol)
            }
        }

        return result
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        doProcessDirectOverriddenCallables(
            functionSymbol, processor, directOverriddenFunctions, superTypesScope,
            FirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        doProcessDirectOverriddenCallables(
            propertySymbol, processor, directOverriddenProperties, superTypesScope,
            FirTypeScope::processDirectOverriddenPropertiesWithBaseScope
        )

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
        superTypesScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        declaredMemberScope.processDeclaredConstructors(processor)
    }

    override fun getCallableNames(): Set<Name> {
        return declaredMemberScope.getContainingCallableNamesIfPresent() + superTypesScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getContainingClassifierNamesIfPresent() + superTypesScope.getClassifierNames()
    }
}
