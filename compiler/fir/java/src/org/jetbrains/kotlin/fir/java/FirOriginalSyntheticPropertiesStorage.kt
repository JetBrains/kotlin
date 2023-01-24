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
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.name.Name

class FirOriginalSyntheticPropertiesStorage(session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    val cacheByOwner: FirCache<FirRegularClassSymbol, FirCache<Name, FirSyntheticPropertySymbol, FirSyntheticPropertySymbol>, Nothing?> =
        cachesFactory.createCache { _ ->
            cachesFactory.createCache { _, symbol -> symbol }
        }

}

val FirSession.originalSyntheticPropertiesStorage: FirOriginalSyntheticPropertiesStorage by FirSession.sessionComponentAccessor()
