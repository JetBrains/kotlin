/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

abstract class DeprecationsProvider {
    abstract fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite?
}

class DeprecationsProviderImpl(
    firCachesFactory: FirCachesFactory,
    private val all: List<DeprecationInfoProvider>?,
    private val bySpecificSite: Map<AnnotationUseSiteTarget, List<DeprecationInfoProvider>>?,
) : DeprecationsProvider() {
    private val cache: FirCache<LanguageVersionSettings, DeprecationsPerUseSite, Nothing?> =
        firCachesFactory.createCache { languageVersionSettings ->
            @Suppress("UNCHECKED_CAST")
            DeprecationsPerUseSite(
                all?.computeDeprecationInfoOrNull(languageVersionSettings),
                bySpecificSite
                    ?.mapValues { (_, info) -> info.computeDeprecationInfoOrNull(languageVersionSettings) }
                    ?.filterValues { it != null } as Map<AnnotationUseSiteTarget, FirDeprecationInfo>?
            )
        }

    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite {
        return cache.getValue(languageVersionSettings, null)
    }

    private fun List<DeprecationInfoProvider>.computeDeprecationInfoOrNull(languageVersionSettings: LanguageVersionSettings): FirDeprecationInfo? {
        return mapNotNull { it.computeDeprecationInfo(languageVersionSettings) }.maxByOrNull { it.deprecationLevel }
    }
}

object EmptyDeprecationsProvider : DeprecationsProvider() {
    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite {
        return EmptyDeprecationsPerUseSite
    }
}

object UnresolvedDeprecationProvider : DeprecationsProvider() {
    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite? {
        return null
    }
}

abstract class DeprecationInfoProvider {
    abstract fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): FirDeprecationInfo?
}

abstract class FirDeprecationInfo : Comparable<FirDeprecationInfo> {
    abstract val deprecationLevel: DeprecationLevelValue
    abstract val propagatesToOverrides: Boolean

    /**
     * This property mustn't be called before the ANNOTATION_ARGUMENTS phase is finished.
     */
    abstract fun getMessage(session: FirSession): String?

    override fun compareTo(other: FirDeprecationInfo): Int {
        val lr = deprecationLevel.compareTo(other.deprecationLevel)
        //to prefer inheritable deprecation
        return if (lr == 0 && !propagatesToOverrides && other.propagatesToOverrides) 1
        else lr
    }
}