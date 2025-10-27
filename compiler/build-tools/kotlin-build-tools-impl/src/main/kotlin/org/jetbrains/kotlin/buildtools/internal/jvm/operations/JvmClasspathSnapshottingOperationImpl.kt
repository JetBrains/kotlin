/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.nio.file.Path

internal class JvmClasspathSnapshottingOperationImpl(
    private val classpathEntry: Path,
) : BuildOperationImpl<ClasspathEntrySnapshot>(), JvmClasspathSnapshottingOperation {

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        options[key] = value
    }

    override val options: Options = Options(JvmClasspathSnapshottingOperation::class)

    override fun executeImpl(projectId: ProjectId, executionPolicy: ExecutionPolicy, logger: KotlinLogger?): ClasspathEntrySnapshot {
        val granularity: ClassSnapshotGranularity = options["GRANULARITY"]
        val parseInlinedLocalClasses: Boolean = options["PARSE_INLINED_LOCAL_CLASSES"]
        val origin = ClasspathEntrySnapshotter.snapshot(
            classpathEntry.toFile(), ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses), getMetricsReporter()
        )
        return ClasspathEntrySnapshotImpl(origin)
    }

    operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        @JvmField
        val GRANULARITY: Option<ClassSnapshotGranularity> = Option("GRANULARITY", ClassSnapshotGranularity.CLASS_MEMBER_LEVEL)

        @JvmField
        val PARSE_INLINED_LOCAL_CLASSES: Option<Boolean> = Option("PARSE_INLINED_LOCAL_CLASSES", true)
    }
}