/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.ClasspathEntrySnapshotImpl
import org.jetbrains.kotlin.buildtools.internal.OptionsDelegate
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.nio.file.Path

class JvmClasspathSnapshottingOperationImpl(
    private val classpathEntry: Path,
) : BuildOperationImpl<ClasspathEntrySnapshot>(), JvmClasspathSnapshottingOperation {

    private val optionsDelegate = OptionsDelegate()

    init {
        this[GRANULARITY] = ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
        this[PARSE_INLINED_LOCAL_CLASSES] = true
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V = optionsDelegate[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    override fun execute(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): ClasspathEntrySnapshot {
        val granularity: ClassSnapshotGranularity = optionsDelegate["GRANULARITY"]
        val parseInlinedLocalClasses: Boolean = optionsDelegate["PARSE_INLINED_LOCAL_CLASSES"]
        val metricsReporter = this[METRICS_COLLECTOR]?.let { BuildMetricsReporterImpl() }
            ?: DoNothingBuildMetricsReporter
        val origin = ClasspathEntrySnapshotter.snapshot(
            classpathEntry.toFile(),
            ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses),
            metricsReporter
        )
        this[METRICS_COLLECTOR]?.let { metricsCollector ->
            metricsReporter.getMetrics().buildTimes.buildTimesMapMs().forEach { (key, value) ->
                metricsCollector.collectMetric(key.name, BuildMetricsCollector.ValueType.MILLISECONDS, value)
            }
        }
        return ClasspathEntrySnapshotImpl(origin)
    }

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> get(key: Option<V>): V = optionsDelegate[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        @JvmField
        val GRANULARITY: Option<ClassSnapshotGranularity> = Option("GRANULARITY")

        @JvmField
        val PARSE_INLINED_LOCAL_CLASSES: Option<Boolean> = Option("PARSE_INLINED_LOCAL_CLASSES")
    }
}
