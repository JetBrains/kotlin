/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.config.LanguageVersionSettings

@NoMutableState
class FirLanguageSettingsComponent(val languageVersionSettings: LanguageVersionSettings) : FirSessionComponent

private val FirSession.languageSettingsComponent: FirLanguageSettingsComponent by FirSession.sessionComponentAccessor()

val FirSession.languageVersionSettings: LanguageVersionSettings
    get() = languageSettingsComponent.languageVersionSettings

