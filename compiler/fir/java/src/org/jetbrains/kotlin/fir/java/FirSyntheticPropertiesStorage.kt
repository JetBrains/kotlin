/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.java.scopes.JavaClassUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext.ResultOfIntersection
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.name.Name

typealias SyntheticPropertiesCache = FirCache<Name, FirSyntheticPropertySymbol?, Pair<JavaClassUseSiteMemberScope, ResultOfIntersection<FirPropertySymbol>>>

class FirSyntheticPropertiesStorage(session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    val cacheByOwner: FirCache<FirRegularClass, SyntheticPropertiesCache, Nothing?> =
        cachesFactory.createCache { _ ->
            cachesFactory.createCache { _, (scope, intersection) ->
                scope.syntheticPropertyFromOverride(intersection)
            }
        }
}

val FirSession.syntheticPropertiesStorage: FirSyntheticPropertiesStorage by FirSession.sessionComponentAccessor()