/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.AbstractProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAndPlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.MetadataProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jsProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.wasmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlin.io.path.writeText

@DisplayName("Test that verify that only all the expected metrics are reported without checking their values")
class SmokeCompilationMetricsTest : BaseCompilationTest() {
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Key compilation metrics are reported on every platform")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testKeyCompilationMetricsAreReportedOnAllPlatforms(project: ProjectCreator) {
        project {
            val module1 = module("basic-multimodule-project/module-1")

            val expectedNames = when (this) {
                is MetadataProject -> Common.nonIncrementalMetricNames + Metadata.compilerTranslationMetrics
                is JvmProject -> Common.nonIncrementalMetricNames + Jvm.compilerTranslationMetrics
                else -> Common.nonIncrementalMetricNames + Klib.compilerTranslationMetrics
            }

            module1.compileWithMetrics { metrics ->
                val actualNames = metrics.all().map { it.name }.toSet()
                assertTrue(actualNames.containsAll(expectedNames)) {
                    "Missing expected metrics.\n\nMissing: ${expectedNames - actualNames}\nGot: $actualNames"
                }
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Basic non-incremental compilation metrics test (JVM only)")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testNonIncrementalCompilationMetricsOnJvm(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module1 = module("basic-multimodule-project/module-1")
            val module2 = module("basic-multimodule-project/module-2", listOf(module1))

            module1.compileWithMetrics { metrics ->
                val expectedNames = Jvm.nonIncrementalMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module1 non-incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
            module2.compileWithMetrics { metrics ->
                val expectedNames = Jvm.nonIncrementalMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module2 non-incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("AKt.class", "BKt.class")
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Basic incremental compilation metrics test")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testIncrementalCompilationMetrics(project: ProjectCreator) {
        project {
            assumeFalse(this is MetadataProject)
            val isJvm = this is JvmProject
            val module1 = module("basic-multimodule-project/module-1")
            val module2 = module("basic-multimodule-project/module-2", listOf(module1))

            module1.compileIncrementallyWithMetrics(SourcesChanges.ToBeCalculated) { metrics ->
                val actualNames = metrics.all().map { it.name }.toSet()
                if (isJvm) {
                    val expectedNames = Jvm.incrementalRecompilationMetricNames
                    assertEquals(expectedNames, actualNames) {
                        "Unexpected set of metric names for module1 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                    }
                    assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
                } else {
                    val expectedNames = Klib.incrementalMetricNames
                    assertTrue(actualNames.containsAll(expectedNames)) {
                        "Missing expected incremental metrics for module1.\n\nMissing: ${expectedNames - actualNames}\nGot: $actualNames"
                    }
                }
            }
            module2.compileIncrementallyWithMetrics(SourcesChanges.ToBeCalculated) { metrics ->
                val actualNames = metrics.all().map { it.name }.toSet()
                if (isJvm) {
                    val expectedNames = Jvm.incrementalRecompilationMetricNames
                    assertEquals(expectedNames, actualNames) {
                        "Unexpected set of metric names for module2 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                    }
                    assertOutputs("AKt.class", "BKt.class")
                } else {
                    val expectedNames = Klib.incrementalMetricNames
                    assertTrue(actualNames.containsAll(expectedNames)) {
                        "Missing expected incremental metrics for module2.\n\nMissing: ${expectedNames - actualNames}\nGot: $actualNames"
                    }
                }
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Metrics are collected even when compilation fails")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testCompilationErrorMetrics(project: ProjectCreator) {
        project {
            val module1 = module("basic-multimodule-project/module-1")

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
    @DisplayName("Second-round incremental compilation metrics (JVM only)")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testSecondRoundIncrementalCompilationMetricsOnJvm(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module1 = module("basic-multimodule-project/module-1")

            module1.compileIncrementallyWithMetrics(SourcesChanges.ToBeCalculated) { metrics ->
                val expectedNames = Jvm.incrementalRecompilationMetricNames
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

                val expectedNames = Jvm.incrementalCompilationMetricNames
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

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Cross-module incremental compilation reports history-based metrics (JS)")
    @TestMetadata("js-ic-basic")
    fun testCrossModuleIncrementalHistoryMetricsOnJs(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            assertCrossModuleIncrementalHistoryMetricsAreReported()
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Cross-module incremental compilation reports history-based metrics (Wasm)")
    @TestMetadata("js-ic-basic")
    fun testCrossModuleIncrementalHistoryMetricsOnWasm(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            assertCrossModuleIncrementalHistoryMetricsAreReported()
        }
    }

    private fun AbstractProject<*, *, *>.assertCrossModuleIncrementalHistoryMetricsAreReported() {
        val libModule = module("js-ic-basic-lib")
        val appModule = module("js-ic-basic-app", dependencies = listOf(libModule))

        libModule.compileIncrementally(SourcesChanges.ToBeCalculated)
        appModule.compileIncrementally(SourcesChanges.ToBeCalculated)

        val modifiedFile = libModule.sourcesDirectory.resolve("A.kt")
        modifiedFile.writeText(
            """
                class A {
                    val x = "a"
                }
            """.trimIndent()
        )
        libModule.compileIncrementally(SourcesChanges.ToBeCalculated) {
            assertCompiledSources("A.kt", "useAInLibMain.kt")
        }

        appModule.compileIncrementallyWithMetrics(
            SourcesChanges.Known(
                modifiedFiles = libModule.outputDirectory.walk().map(Path::toFile).toList(),
                removedFiles = emptyList(),
            )
        ) { metrics ->
            assertCompiledSources("useAInAppMain.kt")

            val expectedNames = Klib.crossModuleIncrementalMetricNames
            val actualNames = metrics.all().map { it.name }.toSet()
            assertTrue(actualNames.containsAll(expectedNames)) {
                "Missing expected cross-module incremental metrics for the app module.\n\nMissing: ${expectedNames - actualNames}\nGot: $actualNames"
            }
        }
    }

    companion object {
        private const val SOURCES_ROUND_COMPILER_TIME = "Run compilation -> Sources compilation round -> Compiler time"

        object Common {
            val nonIncrementalMetricNames = setOf(
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code analysis",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler initialization time",
                "Total compiler iteration",
            )

            val incrementalMetricNames = setOf(
                "Run compilation",
                "Run compilation -> Calculate initial dirty sources set",
                "Run compilation -> Sources compilation round",
                "Run compilation -> Generate compiler reference index",
                "Run compilation -> Store build info",
                "Run compilation -> Update caches",
                "Calculate output size",
                "Total size of the cache directory",
                "Total size of the cache directory -> ABI snapshot size",
                "Total compiler iteration",
            )
        }

        object Jvm {
            val gcMetricNames = setOf(
                "PS MarkSweep",
                "PS Scavenge",
            )

            val compilerTranslationMetrics = setOf(
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler translation to IR",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler Klib metadata writing",
            )

            val nonIncrementalMetricNames = gcMetricNames + setOf(
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code analysis",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler Klib metadata writing",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation -> Compiler IR lowering",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation -> Compiler backend",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler initialization time",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler translation to IR",
                "Total compiler iteration",
                "Total compiler iteration -> Analysis lines per second",
                "Total compiler iteration -> Code generation lines per second",
                "Total compiler iteration -> Number of lines analyzed",
            )

            val incrementalBaseMetricNames = nonIncrementalMetricNames + setOf(
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

            val incrementalRecompilationMetricNames = incrementalBaseMetricNames + setOf(
                "Classpath snapshot not found (Rebuild reason)",
                "Run compilation -> Clear outputs on rebuild",
                "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally -> Load current classpath snapshot -> Remove duplicate classes",
                "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally -> Load current classpath snapshot",
                "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally",
            )

            val incrementalCompilationMetricNames = incrementalBaseMetricNames + setOf(
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

        object Klib {
            val compilerTranslationMetrics = setOf(
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler translation to IR",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler Klib writing",
            )

            val incrementalMetricNames = Common.incrementalMetricNames + setOf(
                "Run compilation -> Write history file",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler translation to IR",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler Klib writing",
            )

            val crossModuleIncrementalMetricNames = incrementalMetricNames + setOf(
                "Run compilation -> Calculate initial dirty sources set -> Analyze dependency changes -> Find history files",
                "Run compilation -> Calculate initial dirty sources set -> Analyze dependency changes -> Analyze history files",
            )
        }

        object Metadata {
            val compilerTranslationMetrics = setOf(
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler Klib metadata writing",
            )
        }

        init {
            require(Jvm.incrementalBaseMetricNames.containsAll(Common.incrementalMetricNames)) {
                "Common.incrementalMetricNames must be a subset of the strict JVM incremental metric names, " +
                        "but these are missing: ${Common.incrementalMetricNames - Jvm.incrementalBaseMetricNames}"
            }
        }
    }
}
