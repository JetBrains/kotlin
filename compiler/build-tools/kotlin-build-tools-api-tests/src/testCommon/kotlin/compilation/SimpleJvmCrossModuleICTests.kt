/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.tests.buildToolsVersion
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunnerProvider
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.prepareModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.exists
import kotlin.io.path.writeText

@DisplayName("Smoke tests for cross-module incremental compilation via the build tools API")
class SimpleJvmCrossModuleICTests : IncrementalBaseCompilationTest() {
    @CompilationTest
    fun compilationAvoidance(buildRunnerProvider: BuildRunnerProvider) {
        maybeSkip(buildRunnerProvider)
        val module1 = prepareModule("jvm-module1", workingDirectory)
        val module2 = prepareModule("jvm-module2", workingDirectory)

        buildRunnerProvider(project).use { runner ->
            module1.compileIncrementally(runner, SourcesChanges.Unknown) { _, compiledSources ->
                if (buildToolsVersion.reportsCompiledSources) {
                    val expectedSources = setOf(
                        "jvm-module1/src/foo.kt",
                        "jvm-module1/src/bar.kt",
                        "jvm-module1/src/baz.kt",
                    )
                    assertEquals(expectedSources, compiledSources)
                }
            }
            module2.compileIncrementally(runner, SourcesChanges.Unknown, dependencies = setOf(module1)) { _, compiledSources ->
                if (buildToolsVersion.reportsCompiledSources) {
                    val expectedSources = setOf(
                        "jvm-module2/src/a.kt",
                        "jvm-module2/src/b.kt",
                    )
                    assertEquals(expectedSources, compiledSources)
                }
            }
        }

        val barKt = module1.sourcesDirectory.resolve("bar.kt")
        // change the form of the bar method
        barKt.writeText(
            """
            class Bar {
                fun bar() {
                    foo()
                }
            }
            """.trimIndent()
        )

        buildRunnerProvider(project).use { runner ->
            module1.compileIncrementally(
                runner,
                SourcesChanges.Known(
                    modifiedFiles = listOf(barKt.toFile()),
                    removedFiles = emptyList()
                )
            ) { _, compiledSources ->
                if (buildToolsVersion.reportsCompiledSources) {
                    assertEquals(setOf("jvm-module1/src/bar.kt"), compiledSources)
                }
            }
            module2.compileIncrementally(
                runner,
                SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList()),
                dependencies = setOf(module1)
            ) { _, compiledSources ->
                // check that nothing is deleted
                assertTrue(module2.outputDirectory.resolve("AKt.class").exists())
                assertTrue(module2.outputDirectory.resolve("BKt.class").exists())
                if (buildToolsVersion.reportsCompiledSources) {
                    assertEquals(emptySet<String>(), compiledSources)
                }
            }
        }
    }
}