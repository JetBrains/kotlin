/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirLookupDefaultStarImportsInSourcesSettingHolder

/**
 * Disables default star import lookup optimization when analyzing Stdlib sources (or another project with enabled `-Xallow-kotlin-package`
 * flag), even if attached as a .jar dependency (i.e., not Stdlib project itself).
 * This is done for all library source analysis sessions because there's no reliable way to distinguish stdlib .jar from other libs.
 */
internal fun LLFirSession.createLookupDefaultStarImportsInSourcesSettingHolder(
    languageVersionSettings: LanguageVersionSettings,
): FirLookupDefaultStarImportsInSourcesSettingHolder {
    val value =
        FirLookupDefaultStarImportsInSourcesSettingHolder.defaultSetting(languageVersionSettings) || isLibrarySourceAnalysisSession()
    return FirLookupDefaultStarImportsInSourcesSettingHolder(value)
}

private fun LLFirSession.isLibrarySourceAnalysisSession(): Boolean =
    this is LLFirLibraryOrLibrarySourceResolvableModuleSession && ktModule is KaLibrarySourceModule
