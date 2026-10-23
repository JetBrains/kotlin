/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.jvm.operations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.ClasspathEntrySnapshotSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.SupportsNewDaemonMode
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyByName
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyByName
import org.jetbrains.kotlin.buildtools.internal.trackers.getMetricsReporter
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.nio.file.Path

@Serializable
internal class JvmClasspathSnapshottingOperationImpl(
    override val classpathEntry: Path,
) : BuildOperationImpl<ClasspathEntrySnapshot>(), JvmClasspathSnapshottingOperation, JvmClasspathSnapshottingOperation.Builder,
    DeepCopyable<JvmClasspathSnapshottingOperation>, SupportsNewDaemonMode<ClasspathEntrySnapshot> {

    override fun toBuilder(): JvmClasspathSnapshottingOperation.Builder = deepCopy()

    override fun build(): JvmClasspathSnapshottingOperation = deepCopy()

    override fun deepCopy(): JvmClasspathSnapshottingOperationImpl =
        JvmClasspathSnapshottingOperationImpl(classpathEntry).also { it.copyFrom(this) }

    private fun copyFrom(other: JvmClasspathSnapshottingOperationImpl) {
        super.copyFrom(other)
        granularity = other.granularity
        parseInlinedLocalClasses = other.parseInlinedLocalClasses
        expandTypeAliases = other.expandTypeAliases
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmClasspathSnapshottingOperation.Option<V>): V = getPropertyByName(propertyMap, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmClasspathSnapshottingOperation.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        setPropertyByName(propertyMap, key.id, value)
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

    @SerialName("GRANULARITY")
    var granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL

    @SerialName("PARSE_INLINED_LOCAL_CLASSES")
    var parseInlinedLocalClasses: Boolean = true

    @SerialName("EXPAND_TYPE_ALIASES")
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
