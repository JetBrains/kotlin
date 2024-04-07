/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo

abstract class DeprecationsProvider {
    abstract fun getDeprecationsInfo(session: FirSession): DeprecationsPerUseSite?
}

class DeprecationsProviderImpl(
    firCachesFactory: FirCachesFactory,
    private val all: List<DeprecationInfoProvider>?,
    private val bySpecificSite: Map<AnnotationUseSiteTarget, List<DeprecationInfoProvider>>?
) : DeprecationsProvider() {
    private val cache: FirCache<FirSession, DeprecationsPerUseSite, Nothing?> = firCachesFactory.createCache { session ->
        @Suppress("UNCHECKED_CAST")
        DeprecationsPerUseSite(
            all?.computeDeprecationInfoOrNull(session),
            bySpecificSite?.mapValues { (_, info) -> info.computeDeprecationInfoOrNull(session) }?.filterValues { it != null }
                    as Map<AnnotationUseSiteTarget, DeprecationInfo>?
        )
    }

    override fun getDeprecationsInfo(session: FirSession): DeprecationsPerUseSite {
        return cache.getValue(session, null)
    }

    private fun List<DeprecationInfoProvider>.computeDeprecationInfoOrNull(session: FirSession): DeprecationInfo? {
        return mapNotNull { it.computeDeprecationInfo(session) }.maxByOrNull { it.deprecationLevel }
    }
}

object EmptyDeprecationsProvider : DeprecationsProvider() {
    override fun getDeprecationsInfo(session: FirSession): DeprecationsPerUseSite {
        return EmptyDeprecationsPerUseSite
    }
}

object UnresolvedDeprecationProvider : DeprecationsProvider() {
    override fun getDeprecationsInfo(session: FirSession): DeprecationsPerUseSite? {
        return null
    }
}

abstract class DeprecationInfoProvider {
    abstract fun computeDeprecationInfo(session: FirSession): DeprecationInfo?
}

