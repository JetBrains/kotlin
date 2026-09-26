/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.forward.tests.SmokeCompilationMetricsTest.Companion.Jvm.daemonMetricNames
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.walk
import kotlin.io.path.writeText

@DisplayName("Test that verify that only all the expected metrics are reported on every platform without checking their values")
class SmokeCompilationMetricsTest : BaseCompilationTest() {
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Key compilation metrics are reported on every platform")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testKeyCompilationMetricsAreReportedOnAllPlatforms(project: ProjectCreator) {
        project {
            val module1 = module("basic-multimodule-project/module-1")

            val expectedNames = when (this) {
                is MetadataProject -> Metadata.nonIncrementalMetricNames
                is JvmProject -> Jvm.nonIncrementalMetricNames
                else -> Klib.nonIncrementalMetricNames
            } + maybeGetDaemonMetricNames()

            module1.compileWithMetrics { metrics ->
                val allNames = metrics.all().map { it.name }.toSet()
                val actualNames = allNames.withoutNestedPreLoweringMetrics()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module1 non-incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                if (IR_PRE_LOWERING in expectedNames) {
                    assertNestedPreLoweringMetricsReported(allNames)
                }
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
                    val expectedNames = Jvm.incrementalRecompilationMetricNames + maybeGetDaemonMetricNames()
                    assertEquals(expectedNames, actualNames) {
                        "Unexpected set of metric names for module1 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                    }
                    assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
                } else {
                    val expectedNames = Klib.incrementalRecompilationMetricNames + maybeGetDaemonMetricNames()
                    val klibActualNames = actualNames.withoutNestedPreLoweringMetrics()
                    assertEquals(expectedNames, klibActualNames) {
                        "Unexpected set of metric names for module1 incremental build.\n\nMissing: ${expectedNames - klibActualNames}\nUnexpected: ${klibActualNames - expectedNames}"
                    }
                    assertNestedPreLoweringMetricsReported(actualNames)
                }
            }
            module2.compileIncrementallyWithMetrics(SourcesChanges.ToBeCalculated) { metrics ->
                val actualNames = metrics.all().map { it.name }.toSet()
                if (isJvm) {
                    val expectedNames = Jvm.incrementalRecompilationMetricNames + maybeGetDaemonMetricNames()
                    assertEquals(expectedNames, actualNames) {
                        "Unexpected set of metric names for module2 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                    }
                    assertOutputs("AKt.class", "BKt.class")
                } else {
                    val expectedNames = Klib.incrementalRecompilationMetricNames + maybeGetDaemonMetricNames()
                    val klibActualNames = actualNames.withoutNestedPreLoweringMetrics()
                    assertEquals(expectedNames, klibActualNames) {
                        "Unexpected set of metric names for module2 incremental build.\n\nMissing: ${expectedNames - klibActualNames}\nUnexpected: ${klibActualNames - expectedNames}"
                    }
                    assertNestedPreLoweringMetricsReported(actualNames)
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

            val expectedNames = Klib.crossModuleIncrementalMetricNames + maybeGetDaemonMetricNames()
            val allNames = metrics.all().map { it.name }.toSet()
            val actualNames = allNames.withoutNestedPreLoweringMetrics()
            assertEquals(expectedNames, actualNames) {
                "Unexpected set of metric names for the app module cross-module incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
            }
            assertNestedPreLoweringMetricsReported(allNames)
        }
    }

    companion object {
        private const val SOURCES_ROUND_COMPILER_TIME = "Run compilation -> Sources compilation round -> Compiler time"
        private const val IR_PRE_LOWERING = "$SOURCES_ROUND_COMPILER_TIME -> Compiler IR pre-lowering"
        private const val KLIB_DIRECTORY_SIZE = "KLIB directory cumulative size"
        private const val KLIB_IR_DIRECTORY_SIZE = "$KLIB_DIRECTORY_SIZE/IR (main)"

        object Common {
            val gcMetricNames = setOf(
                "PS MarkSweep",
                "PS Scavenge",
            )

            val nonIncrementalMetricNames = gcMetricNames + setOf(
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code analysis",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler initialization time",
                "Total compiler iteration",
                "Total compiler iteration -> Analysis lines per second",
                "Total compiler iteration -> Number of lines analyzed",
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
            val nonIncrementalMetricNames = Common.nonIncrementalMetricNames + setOf(
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler Klib metadata writing",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation -> Compiler IR lowering",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation -> Compiler backend",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler code generation",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler translation to IR",
                "Total compiler iteration -> Code generation lines per second",
            )

            val daemonMetricNames = setOf(
                "Increase memory usage",
                "Total memory usage at the end of build",
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
        }

        object Klib {
            val metadataDirectorySizeMetricNames = setOf(
                KLIB_DIRECTORY_SIZE,
                "$KLIB_DIRECTORY_SIZE -> $KLIB_DIRECTORY_SIZE/Manifest file",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_DIRECTORY_SIZE/Metadata",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_DIRECTORY_SIZE/Resources",
            )

            val directorySizeMetricNames = metadataDirectorySizeMetricNames + setOf(
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE/IR bodies",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE/IR declarations",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE/IR file entries",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE/IR files",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE/IR signatures",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE/IR signatures (debug info)",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE/IR strings",
                "$KLIB_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE -> $KLIB_IR_DIRECTORY_SIZE/IR types",
            )

            val nonIncrementalMetricNames = Common.nonIncrementalMetricNames + directorySizeMetricNames + setOf(
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler translation to IR",
                IR_PRE_LOWERING,
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler IR Serialization",
                "$SOURCES_ROUND_COMPILER_TIME -> Compiler Klib writing",
            )

            val incrementalMetricNames = Common.incrementalMetricNames + nonIncrementalMetricNames + setOf(
                "Run compilation -> Write history file",
            )

            val incrementalRecompilationMetricNames = incrementalMetricNames + setOf(
                "Build history file not found (Rebuild reason)",
                "Run compilation -> Clear outputs on rebuild",
            )

            val crossModuleIncrementalMetricNames = incrementalMetricNames + setOf(
                "Run compilation -> Calculate initial dirty sources set -> Analyze dependency changes -> Find history files",
                "Run compilation -> Calculate initial dirty sources set -> Analyze dependency changes -> Analyze history files",
            )
        }

        object Metadata {
            val nonIncrementalMetricNames = Common.nonIncrementalMetricNames + Klib.metadataDirectorySizeMetricNames + setOf(
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

    private fun AbstractProject<out BaseCompilationOperation, out BaseCompilationOperation.Builder, out BaseIncrementalCompilationConfiguration.Builder>.maybeGetDaemonMetricNames(): Set<String> =
        if (this.defaultStrategyConfig is ExecutionPolicy.WithDaemon) daemonMetricNames else emptySet()

    private fun Set<String>.withoutNestedPreLoweringMetrics(): Set<String> =
        filterNot { it.startsWith("$IR_PRE_LOWERING -> ") }.toSet()

    private fun assertNestedPreLoweringMetricsReported(actualNames: Set<String>) {
        assertTrue(actualNames.any { it.startsWith("$IR_PRE_LOWERING -> ") }) {
            "Expected nested metrics under '$IR_PRE_LOWERING', but got none.\n\nGot: $actualNames"
        }
    }
}
