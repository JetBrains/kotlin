/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertNoOutputSetChanges
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName

class IncrementalCompilationSmokeTest : BaseCompilationTest() {
    @DisplayName("IC works with the externally tracked changes, similarly to Gradle")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun multiModuleExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        runMultiModuleTest(strategyConfig, useTrackedModules = false)
    }

    @DisplayName("IC works with the changes tracking via our internal machinery, similarly to Maven")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun multiModuleInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kotlinToolchain = strategyConfig.first
        Assumptions.assumeTrue(
            KotlinToolingVersion(kotlinToolchain.getCompilerVersion()) >= KotlinToolingVersion(2, 1, 20, "Beta1"),
            "Internal tracking is supported only since Kotlin 2.1.20-Beta1: KT-70556, the current version is ${kotlinToolchain.getCompilerVersion()}"
        )
        runMultiModuleTest(strategyConfig, useTrackedModules = true)
    }

    private fun runMultiModuleTest(strategyConfig: CompilerExecutionStrategyConfiguration, useTrackedModules: Boolean) {
        scenario(strategyConfig) {
            val module1 = if (useTrackedModules) {
                trackedModule("jvm-module-1")
            } else {
                module("jvm-module-1")
            }

            val module2 = if (useTrackedModules) {
                trackedModule("jvm-module-2", listOf(module1))
            } else {
                module("jvm-module-2", listOf(module1))
            }

            module1.createPredefinedFile("secret.kt", "new-file")
            module1.replaceFileWithVersion("bar.kt", "add-default-argument")
            module1.deleteFile("baz.kt")
            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "secret.kt", "bar.kt")
                // SecretKt is added, BazKt is removed
                assertOutputs(module, "SecretKt.class", "Bar.class", "FooKt.class")
            }
            module2.compile { module, scenarioModule ->
                assertCompiledSources(module, "b.kt")
                assertNoOutputSetChanges(module, scenarioModule)
            }
        }
    }
}
