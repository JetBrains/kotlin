/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.GcMetric
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import java.rmi.Remote
import java.rmi.RemoteException


@ExperimentalBuildToolsApi
internal interface RemoteBuildMetricsCollector : BuildMetricsCollector, Remote {
    @Throws(RemoteException::class)
    override fun collectMetric(name: String, type: BuildMetricsCollector.ValueType, value: Long)
}

internal interface RemoteBuildMetricsReporter<B : BuildTimeMetric, P : BuildPerformanceMetric> : BuildMetricsReporter<B, P>, Remote {
    @Throws(RemoteException::class)
    override fun startMeasure(time: B)

    @Throws(RemoteException::class)
    override fun endMeasure(time: B)

    @Throws(RemoteException::class)
    override fun addTimeMetricNs(time: B, durationNs: Long)

    @Deprecated("Use addTimeMetricNs instead", ReplaceWith("addTimeMetricNs(time, durationNs)"))
    @Throws(RemoteException::class)
    override fun addTimeMetricMs(time: B, durationMs: Long) = addTimeMetricNs(time, durationMs * 1_000_000)

    @Throws(RemoteException::class)
    override fun addMetric(metric: P, value: Long)

    @Throws(RemoteException::class)
    override fun addTimeMetric(metric: P)

    //Change metric to enum if possible
    @Throws(RemoteException::class)
    override fun addGcMetric(metric: String, value: GcMetric)

    @Throws(RemoteException::class)
    override fun startGcMetric(name: String, value: GcMetric)

    @Throws(RemoteException::class)
    override fun endGcMetric(name: String, value: GcMetric)

    @Throws(RemoteException::class)
    override fun addAttribute(attribute: BuildAttribute)

    @Throws(RemoteException::class)
    override fun getMetrics(): BuildMetrics<in B, P>

    @Throws(RemoteException::class)
    override fun addMetrics(metrics: BuildMetrics<out B, out P>)
}

internal interface RemoteKotlinLogger : KotlinLogger, Remote {
    @get:Throws(RemoteException::class)
    override val isDebugEnabled: Boolean

    @Throws(RemoteException::class)
    override fun error(msg: String, throwable: Throwable?)

    @Throws(RemoteException::class)
    override fun warn(msg: String) {
        warn(msg, null)
    }

    @Throws(RemoteException::class)
    override fun warn(msg: String, throwable: Throwable?)

    @Throws(RemoteException::class)
    override fun info(msg: String)

    @Throws(RemoteException::class)
    override fun debug(msg: String)

    @Throws(RemoteException::class)
    override fun lifecycle(msg: String)
}

internal interface RemoteCompilerMessageRenderer : CompilerMessageRenderer, Remote {
    @Throws(RemoteException::class)
    override fun render(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?
    ): String
}

internal interface RemoteCompilerLookupTracker : CompilerLookupTracker, Remote {
    @Throws(RemoteException::class)
    override fun recordLookup(filePath: String, scopeFqName: String, scopeKind: CompilerLookupTracker.ScopeKind, name: String)

    @Throws(RemoteException::class)
    override fun clear()
}
