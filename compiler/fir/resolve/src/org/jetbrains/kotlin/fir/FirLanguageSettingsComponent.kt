/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

@NoMutableState
class FirLanguageSettingsComponent(val session: FirSession) : FirSessionComponent {
    val languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl.DEFAULT // TODO
}

val FirSession.languageSettingsComponent: FirLanguageSettingsComponent by FirSession.sessionComponentAccessor()

val FirSession.languageVersionSettings: LanguageVersionSettings
    get() = languageSettingsComponent.languageVersionSettings
