/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.tests.buildToolsVersion
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunnerProvider
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.prepareModule
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.exists
import kotlin.io.path.writeText

@DisplayName("Smoke tests for incremental compilation within a single module via the build tools API")
class SimpleJvmIntraModuleICTests : IncrementalBaseCompilationTest() {
    @CompilationTest
    fun smokeTest(buildRunnerProvider: BuildRunnerProvider) {
        maybeSkip(buildRunnerProvider)
        val module = prepareModule("jvm-module1", workingDirectory)

        buildRunnerProvider(project).use { runner ->
            module.compileIncrementally(runner, sourcesChanges = SourcesChanges.Unknown) { _, compiledSources ->
                assertTrue(module.outputDirectory.resolve("FooKt.class").exists())
                assertTrue(module.outputDirectory.resolve("Bar.class").exists())
                assertTrue(module.outputDirectory.resolve("BazKt.class").exists())
                if (buildToolsVersion.reportsCompiledSources) {
                    val expectedSources = setOf(
                        "jvm-module1/src/foo.kt",
                        "jvm-module1/src/bar.kt",
                        "jvm-module1/src/baz.kt",
                    )
                    assertEquals(expectedSources, compiledSources)
                }
            }
        }

        val barKt = module.sourcesDirectory.resolve("bar.kt")
        // replace class with a function
        barKt.writeText(
            """
            fun bar() {
                foo()
            }
            """.trimIndent()
        )

        buildRunnerProvider(project).use { runner ->
            module.compileIncrementally(
                runner, sourcesChanges = SourcesChanges.Known(
                    modifiedFiles = listOf(barKt.toFile()), removedFiles = emptyList()
                )
            ) { _, compiledSources ->
                assertTrue(module.outputDirectory.resolve("FooKt.class").exists())
                assertFalse(module.outputDirectory.resolve("Bar.class").exists())
                assertTrue(module.outputDirectory.resolve("BarKt.class").exists())
                assertTrue(module.outputDirectory.resolve("BazKt.class").exists())
                if (buildToolsVersion.reportsCompiledSources) {
                    val expectedSources = setOf(
                        "jvm-module1/src/bar.kt",
                    )
                    assertEquals(expectedSources, compiledSources)
                }
            }
        }
    }
}