/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.config.FirContextReceiversLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirOptInLanguageVersionSettingsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.config.FirSuppressedDiagnosticsCheckers

object CliOnlyLanguageVersionSettingsCheckers : LanguageVersionSettingsCheckers() {
    override val languageVersionSettingsCheckers: Set<FirLanguageVersionSettingsChecker>
        get() = setOf(
            FirOptInLanguageVersionSettingsChecker,
            FirSuppressedDiagnosticsCheckers,
            FirContextReceiversLanguageVersionSettingsChecker,
        )
}
