/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.LanguageVersionSettingsCheckers

class ComposedLanguageVersionSettingsCheckers : LanguageVersionSettingsCheckers() {

    override val languageVersionSettingsCheckers: Set<FirLanguageVersionSettingsChecker>
        get() = _languageVersionSettingsCheckers

    private val _languageVersionSettingsCheckers: MutableSet<FirLanguageVersionSettingsChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: LanguageVersionSettingsCheckers) {
        _languageVersionSettingsCheckers += checkers.languageVersionSettingsCheckers
    }

}