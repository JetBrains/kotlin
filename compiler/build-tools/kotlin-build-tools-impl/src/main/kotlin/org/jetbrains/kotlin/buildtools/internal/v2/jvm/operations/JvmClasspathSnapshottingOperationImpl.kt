/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.jvm.operations

import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.internal.OptionsDelegate
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.internal.v2.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.v2.jvm.JvmClasspathEntrySnapshotImpl
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.nio.file.Path

class JvmClasspathSnapshottingOperationImpl(
    private val classpathEntry: Path,
) : BuildOperationImpl<JvmClasspathEntrySnapshot>(), JvmClasspathSnapshottingOperation {
    private val optionsDelegate = OptionsDelegate<JvmClasspathSnapshottingOperation.Option<*>>()

    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V = optionsDelegate[key]
    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    override fun execute(executionPolicy: ExecutionPolicy?, logger: KotlinLogger?): JvmClasspathEntrySnapshot {
        val granularity = optionsDelegate.getOrElse(JvmClasspathSnapshottingOperation.GRANULARITY, ClassSnapshotGranularity.CLASS_LEVEL)
        val parseInlinedLocalClasses = optionsDelegate.getOrElse(JvmClasspathSnapshottingOperation.PARSE_INLINED_LOCAL_CLASSES, false)

        val origin = ClasspathEntrySnapshotter.snapshot(
            classpathEntry.toFile(),
            ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses),
            DoNothingBuildMetricsReporter
        )
        return JvmClasspathEntrySnapshotImpl(origin)
    }
}
