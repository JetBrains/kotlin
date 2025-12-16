/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.BuildOperation.Companion.METRICS_COLLECTOR
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation.Companion.PARSE_INLINED_LOCAL_CLASSES
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.currentKotlinStdlibLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName

class SmokeJvmClasspathSnapshottingMetricsTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Basic incremental compilation metrics test")
    fun smokeTestIncrementalCompilationMetrics(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kotlinToolchain = strategyConfig.first
        val executionPolicy = strategyConfig.second
        val snapshottingOperation = kotlinToolchain.jvm.classpathSnapshottingOperationBuilder(currentKotlinStdlibLocation)
        val metricsCollector = TestBuildMetricsCollector()
        snapshottingOperation[METRICS_COLLECTOR] = metricsCollector
        kotlinToolchain.createBuildSession().use {
            it.executeOperation(snapshottingOperation.build(), executionPolicy)
        }
        val actualNames = metricsCollector.all().map { it.name }.toSet()
        val expectedNames = baseExpectedMetricNames + parseInlineLocalClassMetricNames
        assertEquals(expectedNames, actualNames) {
            "Unexpected set of metric names for stdlib classpath snapshot.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Basic incremental compilation metrics test")
    fun smokeTestIncrementalCompilationMetricsNoInlineLocalClassesParsing(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kotlinToolchain = strategyConfig.first
        val executionPolicy = strategyConfig.second
        val snapshottingOperation = kotlinToolchain.jvm.classpathSnapshottingOperationBuilder(currentKotlinStdlibLocation)
        val metricsCollector = TestBuildMetricsCollector()
        snapshottingOperation[METRICS_COLLECTOR] = metricsCollector
        snapshottingOperation[PARSE_INLINED_LOCAL_CLASSES] = false
        kotlinToolchain.createBuildSession().use {
            it.executeOperation(snapshottingOperation.build(), executionPolicy)
        }
        val actualNames = metricsCollector.all().map { it.name }.toSet()
        val expectedNames = baseExpectedMetricNames
        assertEquals(expectedNames, actualNames) {
            "Unexpected set of metric names for stdlib classpath snapshot.\n\nMissing: ${expectedNames - actualNames}\nUnexpected: ${actualNames - expectedNames}"
        }
    }

    companion object {
        private val baseExpectedMetricNames = setOf(
            "Classpath entry snapshot transform -> Load classes (paths only)",
            "Classpath entry snapshot transform -> Snapshot classes -> Load contents of classes",
            "Classpath entry snapshot transform -> Snapshot classes -> Snapshot Java classes",
            "Classpath entry snapshot transform -> Snapshot classes -> Snapshot Kotlin classes",
            "Classpath entry snapshot transform -> Snapshot classes",
        )

        private val parseInlineLocalClassMetricNames = setOf(
            "Classpath entry snapshot transform -> Snapshot classes -> Snapshot inlined classes",
        )
    }
}