/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.assertEquals

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
                val expectedNames = baseMetricNames + incrementalCompilationMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module1 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
            module2.compileIncrementallyWithMetrics(SourcesChanges.ToBeCalculated) { metrics ->
                val expectedNames = baseMetricNames + incrementalCompilationMetricNames
                val actualNames = metrics.all().map { it.name }.toSet()
                assertEquals(expectedNames, actualNames) {
                    "Unexpected set of metric names for module2 incremental build.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
                }
                assertOutputs("AKt.class", "BKt.class")
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

        private val incrementalCompilationMetricNames = setOf(
            "Classpath snapshot not found (Rebuild reason)",
            "Number of times classpath snapshot is loaded -> Number of cache hits when loading classpath entry snapshots",
            "Number of times classpath snapshot is loaded -> Number of cache misses when loading classpath entry snapshots",
            "Number of times classpath snapshot is loaded",
            "Number of times classpath snapshot is shrunk and saved after compilation -> Number of classpath entries",
            "Number of times classpath snapshot is shrunk and saved after compilation -> Size of classpath snapshot",
            "Number of times classpath snapshot is shrunk and saved after compilation -> Size of shrunk classpath snapshot",
            "Number of times classpath snapshot is shrunk and saved after compilation",
            "Calculate output size",
            "Run compilation -> Calculate initial dirty sources set",
            "Run compilation -> Clear outputs on rebuild",
            "Run compilation -> Generate compiler reference index",
            "Run compilation -> Shrink and save current classpath snapshot after compilation -> Save shrunk current classpath snapshot",
            "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally -> Load current classpath snapshot -> Remove duplicate classes",
            "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally -> Load current classpath snapshot",
            "Run compilation -> Shrink and save current classpath snapshot after compilation -> Shrink current classpath snapshot non-incrementally",
            "Run compilation -> Shrink and save current classpath snapshot after compilation",
            "Run compilation -> Sources compilation round",
            "Run compilation -> Store build info",
            "Run compilation -> Update caches",
            "Run compilation",
            "Total size of the cache directory -> ABI snapshot size",
            "Total size of the cache directory",
        )
    }
}