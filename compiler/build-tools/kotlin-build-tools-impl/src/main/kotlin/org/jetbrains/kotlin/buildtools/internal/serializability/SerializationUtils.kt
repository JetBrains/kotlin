/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.SourcesChanges.*
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.InaccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.reflect.KMutableProperty1

internal fun <T, K : Any> K.getPropertyByName(
    propertyAccessors: Map<String, KMutableProperty1<*, out Any?>>,
    name: String,
): T {
    @Suppress("UNCHECKED_CAST")
    return (propertyAccessors[name] ?: error("Property with name $name not found in $this")).getter.call(this) as T
}

internal fun <T, K : Any> K.setPropertyByName(
    propertyAccessors: Map<String, KMutableProperty1<*, out Any?>>,
    name: String,
    value: T,
) {
    (propertyAccessors[name] ?: error("Property with name $name not found in $this")).setter.call(this, value)
}

internal object PathAsStringSerializer : KSerializer<Path> {
    override val descriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.pathString)

    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}

internal object CompilerMessageSourceLocationSerializer : KSerializer<CompilerMessageSourceLocation> {
    override val descriptor: SerialDescriptor = CompilerMessageSourceLocationSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: CompilerMessageSourceLocation,
    ) {
        val surrogate = when (value) {
            is CompilerMessageLocation -> CompilerMessageSourceLocationSurrogate.CompilerMessageLocationSurrogate(
                path = value.path,
                line = value.line,
                column = value.column,
                lineContent = value.lineContent
            )
            is CompilerMessageLocationWithRange -> CompilerMessageSourceLocationSurrogate.CompilerMessageLocationWithRangeSurrogate(
                path = value.path,
                line = value.line,
                column = value.column,
                lineEnd = value.lineEnd,
                columnEnd = value.columnEnd,
                lineContent = value.lineContent
            )
            else -> error("Unexpected compiler message source location type: $value")
        }
        encoder.encodeSerializableValue(CompilerMessageSourceLocationSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): CompilerMessageSourceLocation {
        return when (val surrogate = decoder.decodeSerializableValue(CompilerMessageSourceLocationSurrogate.serializer())) {
            is CompilerMessageSourceLocationSurrogate.CompilerMessageLocationSurrogate -> CompilerMessageLocation.create(
                path = surrogate.path,
                line = surrogate.line,
                column = surrogate.column,
                lineContent = surrogate.lineContent
            )!!
            is CompilerMessageSourceLocationSurrogate.CompilerMessageLocationWithRangeSurrogate -> CompilerMessageLocationWithRange.create(
                path = surrogate.path,
                lineStart = surrogate.line,
                columnStart = surrogate.column,
                lineEnd = surrogate.lineEnd,
                columnEnd = surrogate.columnEnd,
                lineContent = surrogate.lineContent
            )!!
        }
    }
}

@Serializable
internal sealed interface CompilerMessageSourceLocationSurrogate {

    val path: String
    val line: Int
    val column: Int
    val lineEnd: Int get() = -1
    val columnEnd: Int get() = -1
    val lineContent: String?

    @Serializable
    data class CompilerMessageLocationSurrogate(
        override val path: String,
        override val line: Int,
        override val column: Int,
        override val lineContent: String?,
    ) : CompilerMessageSourceLocationSurrogate {
        constructor(from: CompilerMessageLocation) : this(
            from.path,
            from.line,
            from.column,
            from.lineContent
        )
    }

    @Serializable
    data class CompilerMessageLocationWithRangeSurrogate(
        override val path: String,
        override val line: Int,
        override val column: Int,
        override val lineEnd: Int,
        override val columnEnd: Int,
        override val lineContent: String?,
    ) : CompilerMessageSourceLocationSurrogate {
        constructor(from: CompilerMessageLocationWithRange) : this(
            from.path,
            from.line,
            from.column,
            from.lineEnd,
            from.columnEnd,
            from.lineContent
        )
    }
}

// *** Jvm IC serialization ***

@Serializer(forClass = AccessibleClassSnapshot::class)
public object AccessibleClassSnapshotSerializer

@Serializer(forClass = InaccessibleClassSnapshot::class)
public object InaccessibleClassSnapshotSerializer

@Serializer(forClass = ClassSnapshot::class)
public object ClassSnapshotSerializer

@Serializer(forClass = ClasspathEntrySnapshot::class)
public object ClasspathEntrySnapshotSerializer

@Serializable
private sealed interface SourcesChangesSurrogate {
    @Serializable
    @SerialName("Unknown")
    object Unknown : SourcesChangesSurrogate

    @Serializable
    @SerialName("ToBeCalculated")
    object ToBeCalculated : SourcesChangesSurrogate

    @Serializable
    @SerialName("Known")
    class Known(
        val modifiedFiles: List<String>,
        val removedFiles: List<String>,
    ) : SourcesChangesSurrogate
}

internal object SourcesChangesSerializer : KSerializer<SourcesChanges> {
    override val descriptor: SerialDescriptor = SourcesChangesSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SourcesChanges) {
        val surrogate = when (value) {
            is Unknown -> SourcesChangesSurrogate.Unknown
            is ToBeCalculated -> SourcesChangesSurrogate.ToBeCalculated
            is Known -> SourcesChangesSurrogate.Known(
                value.modifiedFiles.map { it.path },
                value.removedFiles.map { it.path }
            )
        }
        encoder.encodeSerializableValue(SourcesChangesSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): SourcesChanges {
        return when (val surrogate = decoder.decodeSerializableValue(SourcesChangesSurrogate.serializer())) {
            is SourcesChangesSurrogate.Unknown -> Unknown
            is SourcesChangesSurrogate.ToBeCalculated -> ToBeCalculated
            is SourcesChangesSurrogate.Known -> Known(
                surrogate.modifiedFiles.map(::File),
                surrogate.removedFiles.map(::File)
            )
        }
    }
}

@Serializable
private class JvmSnapshotBasedIncrementalCompilationConfigurationImplSurrogate(
    @Serializable(with = PathAsStringSerializer::class)
    val workingDirectory: Path,
    @Serializable(with = SourcesChangesSerializer::class)
    val sourcesChanges: SourcesChanges,
    val dependenciesSnapshotFiles: List<Path>,
    val shrunkClasspathSnapshot: Path,
    @SerialName("PRECISE_JAVA_TRACKING")
    val preciseJavaTracking: Boolean = false,
    @SerialName("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES")
    val assuredNoClasspathSnapshotChanges: Boolean = false,
    @SerialName("USE_FIR_RUNNER")
    val useFirRunner: Boolean = false,
    @SerialName("ROOT_PROJECT_DIR")
    val rootProjectDir: Path? = null,
    @SerialName("MODULE_BUILD_DIR")
    val moduleBuildDir: Path? = null,
    @SerialName("BACKUP_CLASSES")
    val backupClasses: Boolean = false,
    @SerialName("KEEP_IC_CACHES_IN_MEMORY")
    val keepIcCachesInMemory: Boolean = false,
    @SerialName("FORCE_RECOMPILATION")
    val forceRecompilation: Boolean = false,
    @SerialName("OUTPUT_DIRS")
    val outputDirs: Set<Path>? = null,
    @SerialName("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM")
    val unsafeIncrementalCompilationForMultiplatform: Boolean = false,
    @SerialName("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION")
    val monotonousIncrementalCompileSetExpansion: Boolean = true,
    @SerialName("TRACK_CONFIGURATION_INPUTS")
    val trackConfigurationInputs: Boolean = false,
)

@Suppress("DEPRECATION_ERROR")
internal object JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer :
    KSerializer<JvmSnapshotBasedIncrementalCompilationConfigurationImpl> {
    override val descriptor: SerialDescriptor =
        JvmSnapshotBasedIncrementalCompilationConfigurationImplSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: JvmSnapshotBasedIncrementalCompilationConfigurationImpl) {
        val surrogate = JvmSnapshotBasedIncrementalCompilationConfigurationImplSurrogate(
            workingDirectory = value.workingDirectory,
            sourcesChanges = value.sourcesChanges,
            dependenciesSnapshotFiles = value.dependenciesSnapshotFiles,
            shrunkClasspathSnapshot = value.shrunkClasspathSnapshot,
            preciseJavaTracking = value.preciseJavaTracking,
            assuredNoClasspathSnapshotChanges = value.assuredNoClasspathSnapshotChanges,
            useFirRunner = value.useFirRunner,
            rootProjectDir = value.rootProjectDir,
            moduleBuildDir = value.moduleBuildDir,
            backupClasses = value.backupClasses,
            keepIcCachesInMemory = value.keepIcCachesInMemory,
            forceRecompilation = value.forceRecompilation,
            outputDirs = value.outputDirs,
            unsafeIncrementalCompilationForMultiplatform = value.unsafeIncrementalCompilationForMultiplatform,
            monotonousIncrementalCompileSetExpansion = value.monotonousIncrementalCompileSetExpansion,
            trackConfigurationInputs = value.trackConfigurationInputs,
        )
        encoder.encodeSerializableValue(JvmSnapshotBasedIncrementalCompilationConfigurationImplSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): JvmSnapshotBasedIncrementalCompilationConfigurationImpl {
        val surrogate = decoder.decodeSerializableValue(JvmSnapshotBasedIncrementalCompilationConfigurationImplSurrogate.serializer())
        return JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
            workingDirectory = surrogate.workingDirectory,
            sourcesChanges = surrogate.sourcesChanges,
            dependenciesSnapshotFiles = surrogate.dependenciesSnapshotFiles,
            shrunkClasspathSnapshot = surrogate.shrunkClasspathSnapshot,
        ).apply {
            preciseJavaTracking = surrogate.preciseJavaTracking
            assuredNoClasspathSnapshotChanges = surrogate.assuredNoClasspathSnapshotChanges
            useFirRunner = surrogate.useFirRunner
            rootProjectDir = surrogate.rootProjectDir
            moduleBuildDir = surrogate.moduleBuildDir
            backupClasses = surrogate.backupClasses
            keepIcCachesInMemory = surrogate.keepIcCachesInMemory
            forceRecompilation = surrogate.forceRecompilation
            outputDirs = surrogate.outputDirs
            unsafeIncrementalCompilationForMultiplatform = surrogate.unsafeIncrementalCompilationForMultiplatform
            monotonousIncrementalCompileSetExpansion = surrogate.monotonousIncrementalCompileSetExpansion
            trackConfigurationInputs = surrogate.trackConfigurationInputs
        }
    }
}
