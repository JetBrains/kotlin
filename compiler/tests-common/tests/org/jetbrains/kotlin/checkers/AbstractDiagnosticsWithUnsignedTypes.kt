/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.test.ConfigurationKind

abstract class AbstractDiagnosticsWithUnsignedTypes : AbstractDiagnosticsTest() {
    override fun getConfigurationKind(): ConfigurationKind {
        return ConfigurationKind.NO_KOTLIN_REFLECT
    }

    override fun defaultLanguageVersionSettings(): LanguageVersionSettings =
        CompilerTestLanguageVersionSettings(
            DEFAULT_DIAGNOSTIC_TESTS_FEATURES,
            ApiVersion.KOTLIN_1_3,
            LanguageVersion.KOTLIN_1_3,
            mapOf(AnalysisFlags.useExperimental to listOf("kotlin.ExperimentalUnsignedTypes"))
        )
}
