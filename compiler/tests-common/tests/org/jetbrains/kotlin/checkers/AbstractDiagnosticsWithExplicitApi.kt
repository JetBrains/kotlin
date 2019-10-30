/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.ConfigurationKind

abstract class AbstractDiagnosticsWithExplicitApi : AbstractDiagnosticsTest() {
    override fun getConfigurationKind(): ConfigurationKind {
        return ConfigurationKind.NO_KOTLIN_REFLECT
    }

    override fun defaultLanguageVersionSettings(): LanguageVersionSettings =
        CompilerTestLanguageVersionSettings(
            DEFAULT_DIAGNOSTIC_TESTS_FEATURES,
            LanguageVersionSettingsImpl.DEFAULT.apiVersion,
            LanguageVersionSettingsImpl.DEFAULT.languageVersion,
            mapOf(AnalysisFlags.explicitApiMode to ExplicitApiMode.STRICT)
        )
}