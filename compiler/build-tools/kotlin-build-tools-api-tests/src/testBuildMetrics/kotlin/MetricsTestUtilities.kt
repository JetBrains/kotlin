/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.BuildOperation.Companion.METRICS_COLLECTOR
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.Module

/**
 * Invokes [Module.compile] with autoconfiguration of [org.jetbrains.kotlin.buildtools.api.BuildOperation.METRICS_COLLECTOR].
 * The passed metrics collector will be accessible in [assertions] as [TestBuildMetricsCollector].
 */
fun Module.compileWithMetrics(
    strategyConfig: ExecutionPolicy = defaultStrategyConfig,
    forceOutput: LogLevel? = null,
    compilationConfigAction: (JvmCompilationOperation.Builder) -> Unit = {},
    compilationAction: (JvmCompilationOperation) -> Unit = {},
    assertions: context(Module) CompilationOutcome.(TestBuildMetricsCollector) -> Unit = {},
): CompilationResult {
    val metricsCollector = TestBuildMetricsCollector()
    return compile(strategyConfig, forceOutput, compilationConfigAction = {
        compilationConfigAction(it)
        it[METRICS_COLLECTOR] = metricsCollector
    }, compilationAction) {
        assertions(metricsCollector)
    }
}

/**
 * Invokes [Module.compileIncrementally] with autoconfiguration of [org.jetbrains.kotlin.buildtools.api.BuildOperation.METRICS_COLLECTOR].
 * The passed metrics collector will be accessible in [assertions] as [TestBuildMetricsCollector].
 */
fun Module.compileIncrementallyWithMetrics(
    sourcesChanges: SourcesChanges,
    strategyConfig: ExecutionPolicy = defaultStrategyConfig,
    forceOutput: LogLevel? = null,
    forceNonIncrementalCompilation: Boolean = false,
    compilationConfigAction: (JvmCompilationOperation.Builder) -> Unit = {},
    compilationAction: (JvmCompilationOperation) -> Unit = {},
    icOptionsConfigAction: (JvmSnapshotBasedIncrementalCompilationConfiguration.Builder) -> Unit = {},
    assertions: context(Module) CompilationOutcome.(TestBuildMetricsCollector) -> Unit = {},
): CompilationResult {
    val metricsCollector = TestBuildMetricsCollector()
    return compileIncrementally(
        sourcesChanges,
        strategyConfig,
        forceOutput,
        forceNonIncrementalCompilation,
        compilationConfigAction = {
            compilationConfigAction(it)
            it[METRICS_COLLECTOR] = metricsCollector
        },
        compilationAction,
        icOptionsConfigAction,
    ) {
        assertions(metricsCollector)
    }
}