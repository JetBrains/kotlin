/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.config.LanguageVersionSettings

@NoMutableState
class FirLanguageSettingsComponent(
    val languageVersionSettings: LanguageVersionSettings,
    val isMetadataCompilation: Boolean,
) : FirSessionComponent

private val FirSession.languageSettingsComponent: FirLanguageSettingsComponent by FirSession.sessionComponentAccessor()

val FirSession.languageVersionSettings: LanguageVersionSettings
    get() = languageSettingsComponent.languageVersionSettings

/**
 * Whether the [FirSession] is used for metadata compilation, as opposed to compilation on a specific platform.
 *
 * In the compiler mode, the property is effectively global, since only one module is compiled at a time.
 *
 * In the Analysis API mode, [isMetadataCompilation] indicates whether the [FirSession] is a metadata session. Because the Analysis API can
 * analyze multiple modules at the same time, the property does not determine any "global mode" of metadata compilation. Rather, it signals
 * to the compiler that *this specific module* has to be analyzed as if we were performing metadata compilation.
 */
val FirSession.isMetadataCompilation: Boolean
    get() = languageSettingsComponent.isMetadataCompilation
