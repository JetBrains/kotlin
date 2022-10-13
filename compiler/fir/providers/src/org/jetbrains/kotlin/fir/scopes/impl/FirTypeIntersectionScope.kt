/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.name.Name

class FirTypeIntersectionScope private constructor(
    session: FirSession,
    overrideChecker: FirOverrideChecker,
    private val scopes: List<FirTypeScope>,
    dispatchReceiverType: ConeSimpleKotlinType,
) : AbstractFirOverrideScope(session, overrideChecker) {
    private val intersectionContext =
        FirTypeIntersectionScopeContext(session, overrideChecker, scopes, dispatchReceiverType, forClassUseSiteScope = false)

    private val absentFunctions: MutableSet<Name> = mutableSetOf()
    private val absentProperties: MutableSet<Name> = mutableSetOf()
    private val absentClassifiers: MutableSet<Name> = mutableSetOf()

    private val overriddenSymbols: MutableMap<FirCallableSymbol<*>, Collection<MemberWithBaseScope<FirCallableSymbol<*>>>> = mutableMapOf()

    private val callableNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        scopes.flatMapTo(mutableSetOf()) { it.getCallableNames() }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        processCallablesByName(name, processor, absentFunctions, FirScope::processFunctionsByName)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        processCallablesByName(name, processor, absentProperties, FirScope::processPropertiesByName)
    }

    private inline fun <D : FirCallableSymbol<*>> processCallablesByName(
        name: Name,
        noinline processor: (D) -> Unit,
        absentNames: MutableSet<Name>,
        processCallables: FirScope.(Name, (D) -> Unit) -> Unit
    ) {
        if (name in absentNames) {
            return
        }

        val callablesWithOverridden = intersectionContext.collectIntersectionResultsForCallables(name, processCallables)

        if (callablesWithOverridden.isEmpty()) {
            absentNames.add(name)
            return
        }

        for (resultOfIntersection in callablesWithOverridden) {
            val symbol = resultOfIntersection.chosenSymbol
            overriddenSymbols[symbol] = resultOfIntersection.overriddenMembers
            processor(symbol)
        }
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        intersectionContext.processClassifiersByNameWithSubstitution(name, absentClassifiers, processor)
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : FirCallableSymbol<*>> getDirectOverriddenSymbols(symbol: S): Collection<MemberWithBaseScope<S>> {
        val intersectionOverride = intersectionContext.intersectionOverrides.getValueIfComputed(symbol)
        val allDirectOverridden = overriddenSymbols[symbol].orEmpty() + intersectionOverride?.let {
            overriddenSymbols[it.member]
        }.orEmpty()
        return allDirectOverridden as Collection<MemberWithBaseScope<S>>
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenCallablesWithBaseScope(
            functionSymbol, processor,
            FirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenCallablesWithBaseScope(
            propertySymbol, processor,
            FirTypeScope::processDirectOverriddenPropertiesWithBaseScope
        )

    private fun <D : FirCallableSymbol<*>> processDirectOverriddenCallablesWithBaseScope(
        callableSymbol: D,
        processor: (D, FirTypeScope) -> ProcessorAction,
        processDirectOverriddenInBaseScope: FirTypeScope.(D, ((D, FirTypeScope) -> ProcessorAction)) -> ProcessorAction
    ): ProcessorAction {
        for ((overridden, baseScope) in getDirectOverriddenSymbols(callableSymbol)) {
            if (overridden === callableSymbol) {
                if (!baseScope.processDirectOverriddenInBaseScope(callableSymbol, processor)) return ProcessorAction.STOP
            } else {
                if (!processor(overridden, baseScope)) return ProcessorAction.STOP
            }
        }

        return ProcessorAction.NEXT
    }

    override fun getCallableNames(): Set<Name> = callableNamesCached

    override fun getClassifierNames(): Set<Name> {
        return scopes.flatMapTo(hashSetOf()) { it.getClassifierNames() }
    }

    override fun toString(): String {
        return "Intersection of [${scopes.joinToString(", ")}]"
    }

    companion object {
        fun prepareIntersectionScope(
            session: FirSession,
            overrideChecker: FirOverrideChecker,
            scopes: List<FirTypeScope>,
            dispatchReceiverType: ConeSimpleKotlinType,
        ): FirTypeScope {
            scopes.singleOrNull()?.let { return it }
            if (scopes.isEmpty()) {
                return Empty
            }
            return FirTypeIntersectionScope(session, overrideChecker, scopes, dispatchReceiverType)
        }
    }
}
