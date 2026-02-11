/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.io.path.readText
import kotlin.io.path.writeText

@DisplayName("Test that verify that only all the expected metrics are reported without checking their values")
class SmokeCompilationMetricsTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Basic non-incremental compilation metrics test")
    @TestMetadata("jvm-module-1")
    fun testNonIncrementalCompilationMetrics(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            val module2 = module("jvm-module-2", listOf(module1))

            module1.compileWithMetrics { metrics ->
                val expectedNames = baseMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module1 non-incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
            module2.compileWithMetrics { metrics ->
                val expectedNames = baseMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module2 non-incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("AKt.class", "BKt.class")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Basic incremental compilation metrics test")
    @TestMetadata("jvm-module-1")
    fun testIncrementalCompilationMetrics(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")
            val module2 = module("jvm-module-2", listOf(module1))

            module1.compileIncrementallyWithMetrics(SourcesChanges.ToBeCalculated) { metrics ->
                val expectedNames = incrementalRecompilationMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module1 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
            module2.compileIncrementallyWithMetrics(SourcesChanges.ToBeCalculated) { metrics ->
                val expectedNames = incrementalRecompilationMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module2 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("AKt.class", "BKt.class")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Metrics are collected even when compilation fails")
    @TestMetadata("jvm-module-1")
    fun testCompilationErrorMetrics(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")

            module1.sourcesDirectory.resolve("bar.kt").writeText(
                """
                class Bar {
                    fun bar() = nonExistentFunction()
                }
                """.trimIndent()
            )

            module1.compileWithMetrics { metrics ->
                expectFail()
                val actualNames = metrics.all().map { it.name }.toSet()
                assertTrue(actualNames.isNotEmpty()) {
                    "Expected metrics to be collected even on compilation failure, but got none"
                }

                assertTrue(actualNames.any { it.startsWith("Run compilation") || it.contains("Compiler time") }) {
                    "Expected at least some compilation-related metrics on failure, but got: $actualNames"
                }
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Second-round incremental compilation metrics")
    @TestMetadata("jvm-module-1")
    fun testSecondRoundIncrementalCompilationMetrics(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module1 = module("jvm-module-1")

            module1.compileIncrementallyWithMetrics(SourcesChanges.ToBeCalculated) { metrics ->
                val expectedNames = incrementalRecompilationMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module1 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }

            val bazKt = module1.sourcesDirectory.resolve("baz.kt")
            bazKt.writeText(bazKt.readText().replace("baz() = 42", "baz() = 99"))

            module1.compileIncrementallyWithMetrics(SourcesChanges.Known(modifiedFiles = listOf(bazKt.toFile()), removedFiles = emptyList())) { metrics ->
                assertCompiledSources("baz.kt")

                val expectedNames = incrementalCompilationMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module1 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }

                val compileIterations = metrics.all()
                    .filter { it.name == "Total compiler iteration" }
                    .sumOf { it.value }
                assertEquals(1L, compileIterations) {
                    "Body-only change should need exactly 1 compile iteration, but got $compileIterations"
                }
            }
        }
    }

    companion object {
        private val baseMetricNames = setOf(
            "PS MarkSweep",
            "PS Scavenge",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler code analysis",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler code generation -> Compiler IR lowering",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler code generation -> Compiler backend",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler code generation",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler initialization time",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler translation to IR",
            "Total compiler iteration",
            "Total compiler iteration -> Analysis lines per second",
            "Total compiler iteration -> Code generation lines per second",
            "Total compiler iteration -> Number of lines analyzed",
        )

        private val incrementalCompilationBaseMetricNames = baseMetricNames + setOf(
            "Number of times classpath snapshot is loaded -> Number of cache hits when loading classpath entry snapshots",
            "Number of times classpath snapshot is loaded -> Number of cache misses when loading classpath entry snapshots",
            "Number of times classpath snapshot is loaded",
            "Number of times classpath snapshot is shrunk and saved after compilation -> Number of classpath entries",
            "Number of times classpath snapshot is shrunk and saved after compilation -> Size of classpath snapshot",
            "Number of times classpath snapshot is shrunk and saved after compilation -> Size of shrunk classpath snapshot",
            "Number of times classpath snapshot is shrunk and saved after compilation",
            "Calculate output size",
            "Run compilation -> Calculate initial dirty sources set",
            "Run compilation -> Generate compiler reference index",
            "Run compilation -> Shrink and save current classpath snapshot after compilation -> Save shrunk current classpath snapshot",
            "Run compilation -> Shrink and save current classpath snapshot after compilation",
            "Run compilation -> Sources compilation round",
            "Run compilation -> Store build info",
            "Run compilation -> Update caches",
            "Run compilation",
            "Total size of the cache directory -> ABI snapshot size",
            "Total size of the cache directory",
        )

        private val incrementalRecompilationMetricNames = incrementalCompilationBaseMetricNames + setOf(
            "Classpath snapshot not found (Rebuild reason)",
            "Run compilation -> Clear outputs on rebuild",
            "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally -> Load current classpath snapshot -> Remove duplicate classes",
            "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally -> Load current classpath snapshot",
            "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally",
        )

        private val incrementalCompilationMetricNames = incrementalCompilationBaseMetricNames + setOf(
            "Number of times classpath changes are computed",
            "Run compilation -> Calculate initial dirty sources set -> Analyze Android layouts",
            "Run compilation -> Calculate initial dirty sources set -> Analyze Java file changes",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Compute changed and impacted set -> Compute class changes -> Compute Java class changes",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Compute changed and impacted set -> Compute class changes -> Compute Kotlin class changes",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Compute changed and impacted set -> Compute class changes",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Compute changed and impacted set",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Load current classpath snapshot -> Remove duplicate classes",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Load current classpath snapshot",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Load shrunk previous classpath snapshot",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Shrink current classpath snapshot -> Find referenced classes",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Shrink current classpath snapshot -> Find transitively referenced classes",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Shrink current classpath snapshot -> Get lookup symbols",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes -> Shrink current classpath snapshot",
            "Run compilation -> Calculate initial dirty sources set -> Compute classpath changes",
            "Run compilation -> Calculate initial dirty sources set -> Detect removed classes"
        )
    }
}