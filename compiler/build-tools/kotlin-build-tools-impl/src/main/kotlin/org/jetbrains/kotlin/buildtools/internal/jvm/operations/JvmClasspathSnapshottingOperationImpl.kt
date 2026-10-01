/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.nio.file.Path

@Serializable
internal class JvmClasspathSnapshottingOperationImpl(
    override val classpathEntry: Path,
    @SerialName("GRANULARITY") internal var granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL,
    @SerialName("PARSE_INLINED_LOCAL_CLASSES") internal var parseInlinedLocalClasses: Boolean = true,
    @SerialName("EXPAND_TYPE_ALIASES") internal var expandTypeAliases: Boolean = false,
) : BuildOperationImpl<ClasspathEntrySnapshot>(), JvmClasspathSnapshottingOperation, JvmClasspathSnapshottingOperation.Builder,
    DeepCopyable<JvmClasspathSnapshottingOperation> {

    override fun toBuilder(): JvmClasspathSnapshottingOperation.Builder = deepCopy()

    override fun build(): JvmClasspathSnapshottingOperation = deepCopy()

    override fun deepCopy(): JvmClasspathSnapshottingOperationImpl =
        JvmClasspathSnapshottingOperationImpl(classpathEntry, granularity, parseInlinedLocalClasses, expandTypeAliases).also {
            it.copyFrom(this)
        }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V =
        JvmClasspathSnapshottingOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        JvmClasspathSnapshottingOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    override val usesApplicationEnvironment: Boolean
        get() = false

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext,
    ): ClasspathEntrySnapshot {
        val granularity: ClassSnapshotGranularity = get(GRANULARITY)
        val parseInlinedLocalClasses: Boolean = get(PARSE_INLINED_LOCAL_CLASSES)
        val expandTypeAliases: Boolean = get(EXPAND_TYPE_ALIASES)
        val origin = ClasspathEntrySnapshotter.snapshot(
            classpathEntry.toFile(),
            ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses, expandTypeAliases),
            getMetricsReporter()
        )
        return ClasspathEntrySnapshotImpl(origin)
    }

    operator fun <V> get(key: Option<V>): V =
        JvmClasspathSnapshottingOperationImpl::class.getPropertyWithSerialNameValue(this, key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        JvmClasspathSnapshottingOperationImpl::class.setPropertyWithSerialNameValue(this, key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    companion object {
        val GRANULARITY: Option<ClassSnapshotGranularity> = Option("GRANULARITY")

        val PARSE_INLINED_LOCAL_CLASSES: Option<Boolean> = Option("PARSE_INLINED_LOCAL_CLASSES")

        val EXPAND_TYPE_ALIASES: Option<Boolean> = Option("EXPAND_TYPE_ALIASES")
    }
}
