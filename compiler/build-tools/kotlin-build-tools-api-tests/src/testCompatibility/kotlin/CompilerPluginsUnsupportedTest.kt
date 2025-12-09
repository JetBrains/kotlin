/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

class CompilerPluginsUnsupportedTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("The new compiler plugins argument is unsupported with proper message")
    @TestMetadata("jvm-module-1")
    fun testCompatibilityCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kotlinToolchain = strategyConfig.first
        assumeTrue(KotlinToolingVersion(kotlinToolchain.getCompilerVersion()) < KotlinToolingVersion(2, 3, 20, "dev-6376"))
        project(strategyConfig) {
            val module = module("compiler-plugins")
            val exception = assertThrows<IllegalStateException> {
                module.compile(compilationConfigAction = { it.compilerArguments[COMPILER_PLUGINS] = emptyList() })
            }

            assertEquals("COMPILER_PLUGINS is available only since 2.3.20", exception.message)
        }
    }
}