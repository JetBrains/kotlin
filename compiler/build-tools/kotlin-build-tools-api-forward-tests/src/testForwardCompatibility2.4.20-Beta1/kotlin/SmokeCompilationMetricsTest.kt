/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.api.BuildOperation.Companion.METRICS_COLLECTOR
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.BtaV2StrategyAndPlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.JvmProject
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.MetadataProject
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.ProjectCreator
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import java.util.concurrent.ConcurrentHashMap

@DisplayName("Test that verify that the expected metrics are reported on every platform")
class SmokeCompilationMetricsTest : BaseCompilationTest() {
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Key compilation metrics are reported on every platform")
    fun keyCompilationMetricsAreReportedOnAllPlatforms(project: ProjectCreator) {
        project {
            val module1 = module("basic-multimodule-project/module-1")

            // This test does not pin the full metric set (which is shaped after the JVM build) or assert `.class`
            // outputs. It only checks that the platform-independent compiler-phase metrics are reported, so it holds
            // for JVM/JS/Wasm/metadata.
            val reportedNames = ConcurrentHashMap.newKeySet<String>()
            val metricsCollector = object : BuildMetricsCollector {
                override fun collectMetric(name: String, type: BuildMetricsCollector.ValueType, value: Long) {
                    reportedNames += name
                }
            }

            val expectedNames = when (this) {
                // Every IR-producing platform (JVM/JS/Wasm) reports "Compiler translation to IR"; only the metadata
                // compiler does not, since it produces no IR. So we require it everywhere except on metadata.
                is MetadataProject -> platformIndependentMetricNames + compilerTranslationToMetadataMetrics
                is JvmProject -> platformIndependentMetricNames + compilerTranslationToJvmMetrics
                else -> platformIndependentMetricNames + compilerTranslationToKlibMetrics
            }
            module1.compile(compilationConfigAction = {
                it[METRICS_COLLECTOR] = metricsCollector
            }) {
                assertTrue(reportedNames.containsAll(expectedNames)) {
                    "Missing expected metrics.\n\nMissing: ${expectedNames - reportedNames}\nGot: $reportedNames"
                }
            }
        }
    }

    companion object {
        // Reported by every IR-producing Klib platform (JS/Wasm/Naitve).
        // This excludes the metadata compiler, which produces no IR, and JVM, which produces no Klib.
        private val compilerTranslationToKlibMetrics = setOf(
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler translation to IR",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler Klib writing",
        )

        // Reported by the metadata compiler, which DO NOT produce IR and produce Klib (with metadata).
        private val compilerTranslationToMetadataMetrics = setOf(
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler Klib metadata writing",
        )

        // Reported on the JVM platform, which DOES produce IR and produce Klib (with metadata).
        private val compilerTranslationToJvmMetrics = setOf(
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler translation to IR",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler Klib metadata writing",
        )

        // Compiler-phase metrics that are reported regardless of the target platform (JVM/JS/Wasm/metadata).
        // Deliberately excludes:
        //  - GC metrics ("PS MarkSweep"/"PS Scavenge"), which depend on whether the GC actually ran;
        //  - JVM-only classpath-snapshot metrics;
        //  - "Compiler code generation", which is JVM-only (the klib platforms serialize IR instead);
        //  - "Compiler translation to IR", which is added per-platform (see COMPILER_TRANSLATION_TO_IR_METRIC): the
        //    metadata compiler does not report it (it produces no IR), so it is required only for JVM/JS/Wasm.
        //  - Klib writing. This diagnostic is slightly different on JVM and on klib platforms.
        private val platformIndependentMetricNames = setOf(
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler code analysis",
            "Run compilation -> Sources compilation round -> Compiler time -> Compiler initialization time",
            "Total compiler iteration",
        )
    }
}
