/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.SimpleDeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.isFulfilled
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

abstract class DeprecationsProvider {
    abstract fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite?
}

class DeprecationsProviderImpl(
    firCachesFactory: FirCachesFactory,
    private val all: List<DeprecationAnnotationInfo>?,
    private val bySpecificSite: Map<AnnotationUseSiteTarget, List<DeprecationAnnotationInfo>>?
) : DeprecationsProvider() {
    private val cache: FirCache<LanguageVersionSettings, DeprecationsPerUseSite, Nothing?> = firCachesFactory.createCache { version ->
        @Suppress("UNCHECKED_CAST")
        DeprecationsPerUseSite(
            all?.computeDeprecationInfoOrNull(version),
            bySpecificSite?.mapValues { (_, info) -> info.computeDeprecationInfoOrNull(version) }?.filterValues { it != null }
                    as Map<AnnotationUseSiteTarget, DeprecationInfo>?
        )
    }

    override fun getDeprecationsInfo(languageVersionSettings: LanguageVersionSettings): DeprecationsPerUseSite {
        return cache.getValue(languageVersionSettings, null)
    }

    private fun List<DeprecationAnnotationInfo>.computeDeprecationInfoOrNull(version: LanguageVersionSettings): DeprecationInfo? {
        return firstNotNullOfOrNull { it.computeDeprecationInfo(version) }
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

sealed class DeprecationAnnotationInfo {
    abstract fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): DeprecationInfo?
}

data class FutureApiDeprecationInfo(
    override val deprecationLevel: DeprecationLevelValue,
    override val propagatesToOverrides: Boolean,
    val sinceVersion: ApiVersion,
) : DeprecationInfo() {
    override val message: String? get() = null
}

data class RequireKotlinDeprecationInfo(
    override val deprecationLevel: DeprecationLevelValue,
    val versionRequirement: VersionRequirement
) : DeprecationInfo() {
    override val message: String? get() = versionRequirement.message
    override val propagatesToOverrides: Boolean get() = false
}

class RequireKotlinInfo(private val versionRequirement: VersionRequirement) : DeprecationAnnotationInfo() {
    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): DeprecationInfo? {
        return runUnless(versionRequirement.isFulfilled(languageVersionSettings)) {
            RequireKotlinDeprecationInfo(
                deprecationLevel = when (versionRequirement.level) {
                    DeprecationLevel.WARNING -> DeprecationLevelValue.WARNING
                    DeprecationLevel.ERROR -> DeprecationLevelValue.ERROR
                    DeprecationLevel.HIDDEN -> DeprecationLevelValue.HIDDEN
                },
                versionRequirement = versionRequirement
            )
        }
    }
}

class SinceKotlinInfo(val sinceVersion: ApiVersion) : DeprecationAnnotationInfo() {
    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): DeprecationInfo? {
        return runUnless(sinceVersion <= languageVersionSettings.apiVersion) {
            FutureApiDeprecationInfo(
                deprecationLevel = DeprecationLevelValue.HIDDEN,
                propagatesToOverrides = true,
                sinceVersion = sinceVersion,
            )
        }
    }
}

class DeprecatedInfo(
    val level: DeprecationLevelValue,
    val propagatesToOverride: Boolean,
    val message: String?
) : DeprecationAnnotationInfo() {
    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): DeprecationInfo {
        return SimpleDeprecationInfo(
            level,
            propagatesToOverride,
            message
        )
    }
}

class DeprecatedSinceKotlinInfo(
    val warningVersion: ApiVersion?,
    val errorVersion: ApiVersion?,
    val hiddenVersion: ApiVersion?,
    val message: String?,
    val propagatesToOverride: Boolean
) : DeprecationAnnotationInfo() {
    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): DeprecationInfo? {
        fun ApiVersion.takeLevelIfDeprecated(level: DeprecationLevelValue) = level.takeIf { this <= languageVersionSettings.apiVersion }

        val appliedLevel = hiddenVersion?.takeLevelIfDeprecated(DeprecationLevelValue.HIDDEN)
            ?: errorVersion?.takeLevelIfDeprecated(DeprecationLevelValue.ERROR)
            ?: warningVersion?.takeLevelIfDeprecated(DeprecationLevelValue.WARNING)

        return appliedLevel?.let {
            SimpleDeprecationInfo(it, propagatesToOverride, message)
        }
    }
}
