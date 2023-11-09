/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunnerProvider
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.prepareModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.exists

@DisplayName("Smoke tests for non-incremental compilation via the build tools API")
internal class NonIncrementalJvmCompilationTest : BaseCompilationTest() {
    @CompilationTest
    fun smokeSingleModuleTest(buildRunnerProvider: BuildRunnerProvider) {
        val module = prepareModule("jvm-module1", workingDirectory)

        buildRunnerProvider(project).use { runner ->
            module.compile(runner) {
                assertTrue(module.outputDirectory.resolve("FooKt.class").exists())
                assertTrue(module.outputDirectory.resolve("Bar.class").exists())
                assertTrue(module.outputDirectory.resolve("BazKt.class").exists())
            }
        }
    }

    @CompilationTest
    fun smokeMultipleModulesTest(buildRunnerProvider: BuildRunnerProvider) {
        val module1 = prepareModule("jvm-module1", workingDirectory)
        val module2 = prepareModule("jvm-module2", workingDirectory)

        buildRunnerProvider(project).use { runner ->
            module1.compile(runner) {
                assertTrue(module1.outputDirectory.resolve("FooKt.class").exists())
                assertTrue(module1.outputDirectory.resolve("Bar.class").exists())
                assertTrue(module1.outputDirectory.resolve("BazKt.class").exists())
            }
            module2.compile(runner, dependencies = setOf(module1)) {
                assertTrue(module2.outputDirectory.resolve("AKt.class").exists())
                assertTrue(module2.outputDirectory.resolve("BKt.class").exists())
            }
        }
    }
}