/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

/**
 * Determines whether to disable the optimization that skips looking up for declarations from default star imports (e.g., kotlin.*)
 * in module sources.
 */
class FirLookupDefaultStarImportsInSourcesSettingHolder(internal val value: Boolean) : FirSessionComponent {
    companion object {
        fun defaultSetting(languageVersionSettings: LanguageVersionSettings): Boolean =
            languageVersionSettings.getFlag(AnalysisFlags.allowKotlinPackage)

        fun createDefault(languageVersionSettings: LanguageVersionSettings): FirLookupDefaultStarImportsInSourcesSettingHolder =
            FirLookupDefaultStarImportsInSourcesSettingHolder(defaultSetting(languageVersionSettings))
    }
}

private val FirSession.lookupDefaultStarImportsInSourcesSettingHolder: FirLookupDefaultStarImportsInSourcesSettingHolder by FirSession.sessionComponentAccessor()
internal val FirSession.lookupDefaultStarImportsInSources get() = lookupDefaultStarImportsInSourcesSettingHolder.value
