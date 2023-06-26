/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*

fun ConeClassLikeLookupTag.getNestedClassifierScope(session: FirSession, scopeSession: ScopeSession): FirContainingNamesAwareScope? {
    val klass = toSymbol(session)?.fir as? FirRegularClass ?: return null
    return klass.scopeProvider.getNestedClassifierScope(klass, session, scopeSession)
}

/**
 * Use this function to collect direct overridden tree for debug purposes
 */
@Suppress("unused")
@TestOnly
fun debugCollectOverrides(symbol: FirCallableSymbol<*>, session: FirSession, scopeSession: ScopeSession): Map<Any, Any> {
    val scope = symbol.dispatchReceiverType?.scope(
        session,
        scopeSession,
        FakeOverrideTypeCalculator.DoNothing,
        requiredMembersPhase = FirResolvePhase.STATUS,
    ) ?: return emptyMap()

    return debugCollectOverrides(symbol, scope)
}

@TestOnly
fun debugCollectOverrides(symbol: FirCallableSymbol<*>, scope: FirTypeScope): Map<Any, Any> {
    fun process(scope: FirTypeScope, symbol: FirCallableSymbol<*>): Map<Any, Any> {
        val result = mutableMapOf<Any, Any>()
        val resultList = mutableListOf<Any>()
        when (symbol) {
            is FirNamedFunctionSymbol -> scope.processDirectOverriddenFunctionsWithBaseScope(symbol) { baseSymbol, baseScope ->
                resultList.add(process(baseScope, baseSymbol))
                ProcessorAction.NEXT
            }
            is FirPropertySymbol -> scope.processDirectOverriddenPropertiesWithBaseScope(symbol) { baseSymbol, baseScope ->
                resultList.add(process(baseScope, baseSymbol))
                ProcessorAction.NEXT
            }
        }
        result[symbol] = resultList
        return result
    }
    return process(scope, symbol)
}

fun FirNamedFunctionSymbol.overriddenFunctions(
    containingClass: FirClassSymbol<*>,
    session: FirSession,
    scopeSession: ScopeSession,
): List<FirFunctionSymbol<*>> {
    val firTypeScope = containingClass.unsubstitutedScope(
        session,
        scopeSession,
        withForcedTypeCalculator = true,
        memberRequiredPhase = FirResolvePhase.STATUS,
    )

    val overriddenFunctions = mutableListOf<FirFunctionSymbol<*>>()
    firTypeScope.processFunctionsByName(callableId.callableName) { }
    firTypeScope.processOverriddenFunctions(this) {
        overriddenFunctions.add(it)
        ProcessorAction.NEXT
    }

    return overriddenFunctions
}
