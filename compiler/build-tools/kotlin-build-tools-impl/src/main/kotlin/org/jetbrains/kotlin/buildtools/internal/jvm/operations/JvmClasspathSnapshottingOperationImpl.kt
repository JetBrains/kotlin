/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.daemon.ClasspathEntrySnapshotSerializer
import org.jetbrains.kotlin.buildtools.internal.daemon.cast
import org.jetbrains.kotlin.buildtools.internal.daemon.throwPropertyNotFound
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer
import org.jetbrains.kotlin.daemon.common.serialization.PathAsStringSerializer
import org.jetbrains.kotlin.daemon.common.serialization.SupportsNewDaemonMode
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.nio.file.Path

@Serializable
internal class JvmClasspathSnapshottingOperationImpl : BuildOperationImpl<ClasspathEntrySnapshot>, JvmClasspathSnapshottingOperation, JvmClasspathSnapshottingOperation.Builder,
    DeepCopyable<JvmClasspathSnapshottingOperation>, SupportsNewDaemonMode<ClasspathEntrySnapshot> {

    override val classpathEntry: Path

    private constructor(other: JvmClasspathSnapshottingOperationImpl) : super(other) {
        classpathEntry = other.classpathEntry
        granularity = other.granularity
        parseInlinedLocalClasses = other.parseInlinedLocalClasses
        expandTypeAliases = other.expandTypeAliases
    }

    constructor(classpathEntry: Path) : super() {
        this.classpathEntry = classpathEntry
    }

    override fun toBuilder(): JvmClasspathSnapshottingOperation.Builder = deepCopy()

    override fun build(): JvmClasspathSnapshottingOperation = deepCopy()

    override fun deepCopy(): JvmClasspathSnapshottingOperationImpl = JvmClasspathSnapshottingOperationImpl(this)

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V = when (key.id) {
        "GRANULARITY" -> granularity
        "PARSE_INLINED_LOCAL_CLASSES" -> parseInlinedLocalClasses
        "EXPAND_TYPE_ALIASES" -> expandTypeAliases
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            "GRANULARITY" -> granularity = value.cast()
            "PARSE_INLINED_LOCAL_CLASSES" -> parseInlinedLocalClasses = value.cast()
            "EXPAND_TYPE_ALIASES" -> expandTypeAliases = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    override val usesApplicationEnvironment: Boolean
        get() = false

    override fun executeImpl(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        logger: KotlinLogger?,
        executionContext: ExecutionContext
    ): ClasspathEntrySnapshot {
        val origin = ClasspathEntrySnapshotter.snapshot(
            classpathEntry.toFile(),
            ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses, expandTypeAliases),
            getMetricsReporter()
        )
        return ClasspathEntrySnapshotImpl(origin)
    }

    var granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL

    var parseInlinedLocalClasses: Boolean = true

    var expandTypeAliases: Boolean = false
    override fun deserializeResult(serializedData: SerializedDataContainer): ClasspathEntrySnapshot {
        return KotlinToolchainsImpl.serializer.deserialize(ClasspathEntrySnapshotSerializer, serializedData)
    }

    override fun serializeResult(result: ClasspathEntrySnapshot): SerializedDataContainer {
        return KotlinToolchainsImpl.serializer.serialize(ClasspathEntrySnapshotSerializer, result)
    }

    companion object {
        private val propertyMap = mapOf(
            "GRANULARITY" to JvmClasspathSnapshottingOperationImpl::granularity,
            "PARSE_INLINED_LOCAL_CLASSES" to JvmClasspathSnapshottingOperationImpl::parseInlinedLocalClasses,
            "EXPAND_TYPE_ALIASES" to JvmClasspathSnapshottingOperationImpl::expandTypeAliases
        )
    }
}
