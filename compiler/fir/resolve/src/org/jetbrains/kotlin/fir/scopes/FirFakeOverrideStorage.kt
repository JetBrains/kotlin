/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.resolve.ScopeSessionKey
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScope
import org.jetbrains.kotlin.fir.scopes.impl.MemberWithBaseScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType

class FirFakeOverrideStorage(val session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    val substitutionOverrideCacheByScope: FirCache<ScopeSessionKey<*, *>, SubstitutionOverrideCache, Nothing?> =
        cachesFactory.createCache { _ -> SubstitutionOverrideCache(session.firCachesFactory) }

    class SubstitutionOverrideCache(cachesFactory: FirCachesFactory) {
        val overridesForFunctions: FirCache<FirNamedFunctionSymbol, FirNamedFunctionSymbol, FirClassSubstitutionScope> =
            cachesFactory.createCache { original, scope -> scope.createSubstitutionOverrideFunction(original) }
        val overridesForConstructors: FirCache<FirConstructorSymbol, FirConstructorSymbol, FirClassSubstitutionScope> =
            cachesFactory.createCache { original, scope -> scope.createSubstitutionOverrideConstructor(original) }
        val overridesForVariables: FirCache<FirVariableSymbol<*>, FirVariableSymbol<*>, FirClassSubstitutionScope> =
            cachesFactory.createCache { original, scope ->
                when (original) {
                    is FirPropertySymbol -> scope.createSubstitutionOverrideProperty(original)
                    is FirFieldSymbol -> scope.createSubstitutionOverrideField(original)
                    is FirAccessorSymbol -> scope.createSubstitutionOverrideAccessor(original)
                    else -> error("symbol $original is not overridable")
                }
            }
    }

    class IntersectionOverrideCache(cachesFactory: FirCachesFactory) {
        val intersectionOverrides: FirCache<FirCallableSymbol<*>, MemberWithBaseScope<FirCallableSymbol<*>>, ContextForIntersectionOverrideConstruction<*>> =
            cachesFactory.createCache { mostSpecific, context ->
                val (intersectionScope, extractedOverrides, scopeForMostSpecific) = context
                intersectionScope.createIntersectionOverride(extractedOverrides, mostSpecific, scopeForMostSpecific)
            }
    }

    data class ContextForIntersectionOverrideConstruction<D : FirCallableSymbol<*>>(
        val intersectionScope: FirTypeIntersectionScope,
        val extractedOverrides: List<MemberWithBaseScope<D>>,
        val scopeForMostSpecific: FirTypeScope
    )

    val intersectionOverrideCacheByScope: FirCache<ConeKotlinType, IntersectionOverrideCache, Nothing?> =
        cachesFactory.createCache { _ -> IntersectionOverrideCache(cachesFactory) }
}

val FirSession.fakeOverrideStorage: FirFakeOverrideStorage by FirSession.sessionComponentAccessor()
