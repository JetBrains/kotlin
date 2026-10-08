/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)
@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.*
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker.ScopeKind
import org.jetbrains.kotlin.buildtools.api.wasm.WasmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.LogLevel
import org.jetbrains.kotlin.buildtools.internal.js.JsHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsDtsGenerationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsLinkingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmClasspathSnapshottingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.metadata.operations.KotlinMetadataKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.WasmHistoryBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmLinkingOperationImpl
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import org.jetbrains.kotlin.buildtools.api.wasm.IncrementalModule as WasmIncrementalModule


internal fun KClass<*>.findPropertyWithSerialName(name: String): KMutableProperty<*> {
    return this.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>()
        .firstOrNull { it.annotations.any { ann -> ann is SerialName && ann.value == name } }
        ?: error("Property with serial name $name not found in $this")
}

internal fun <T, K : Any> KClass<K>.getPropertyWithSerialNameValue(obj: K, name: String): T {
    @Suppress("UNCHECKED_CAST")
    return (this.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>()
        .firstOrNull { it.annotations.any { ann -> ann is SerialName && ann.value == name } }
        ?: error("Property with serial name $name not found in $this"))
        .getter.call(obj) as T
}

internal fun <T, K : Any> KClass<K>.setPropertyWithSerialNameValue(obj: K, name: String, value: T) {
    (this.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>()
        .firstOrNull { it.annotations.any { ann -> ann is SerialName && ann.value == name } }
        ?: error("Property with serial name $name not found in $this"))
        .setter.call(obj, value)
}

internal object PathAsStringSerializer : KSerializer<Path> {
    override val descriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.pathString)

    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}

internal object ListOfPathsAsStringSerializer : KSerializer<List<Path>> by ListSerializer(PathAsStringSerializer)

internal object SetOfPathsAsStringSerializer : KSerializer<Set<Path>> by SetSerializer(PathAsStringSerializer)

internal object CompilationResultSerializer : KSerializer<CompilationResult> {
    override val descriptor = PrimitiveSerialDescriptor("CompilationResult", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CompilationResult) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): CompilationResult = CompilationResult.valueOf(decoder.decodeString())
}

public val btaSerializersModule: SerializersModule = SerializersModule {
    polymorphic(BtaSerializable::class) {
        subclass(JvmCompilationOperationImpl::class)
        subclass(JvmClasspathSnapshottingOperationImpl::class)
        subclass(JsKlibCompilationOperationImpl::class)
        subclass(WasmKlibCompilationOperationImpl::class)
        subclass(KotlinMetadataKlibCompilationOperationImpl::class)
        subclass(WasmLinkingOperationImpl::class)
        subclass(JsLinkingOperationImpl::class)
        subclass(JsDtsGenerationOperationImpl::class)
//        subclass(JvmCompilationOperationImpl::class)
    }
    polymorphic(CompilerMessageSourceLocation::class) {
        subclass(org.jetbrains.kotlin.buildtools.internal.serializability.CompilerMessageLocation::class)
        subclass(org.jetbrains.kotlin.buildtools.internal.serializability.CompilerMessageLocationWithRange::class)
    }

    polymorphic(JvmIncrementalCompilationConfiguration::class) {
        subclass(
            JvmSnapshotBasedIncrementalCompilationConfigurationImpl::class,
            JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer
        )
    }

    polymorphic(JsIncrementalCompilationConfiguration::class) {
        subclass(
            JsHistoryBasedIncrementalCompilationConfigurationImpl::class
        )
    }
    polymorphic(WasmIncrementalCompilationConfiguration::class) {
        subclass(
            WasmHistoryBasedIncrementalCompilationConfigurationImpl::class
        )
    }
}

@Serializable
public sealed class Message {

    @Serializable
    public data class LogLine(
        public val level: LogLevel,
        public val logLine: String,
    ) : Message()

    @Serializable
    public data class LookupMessage(
        public val filePath: String,
        public val scopeFqName: String,
        public val scopeKind: ScopeKind,
        public val name: String,
    ) : Message()

    @Serializable
    public object LookupClear : Message()

    @Serializable
    public data class CompilerMessageWithDiagnosticId(
        public val severity: CompilerMessageSeverity,
        public val message: String,
        public val location: CompilerMessageSourceLocation? = null,
        public val diagnosticId: String?,
    ) : Message()

    @Serializable
    public object CompilerMessageClear : Message()

    @Serializable
    public data class CollectMetricMessage(
        public val name: String,
        public val type: BuildMetricsCollector.ValueType,
        public val value: Long,
    ) : Message()
}

public fun CompilerMessageSourceLocation.beforeSerialization(): CompilerMessageSourceLocation =
    when (this) {
        is CompilerMessageLocation -> CompilerMessageLocation(this)
        is CompilerMessageLocationWithRange -> CompilerMessageLocationWithRange(this)
        else -> this
    }

public fun CompilerMessageSourceLocation.afterSerialization(): CompilerMessageSourceLocation? =
    when (this) {
        is org.jetbrains.kotlin.buildtools.internal.serializability.CompilerMessageLocation -> this.toCompiler()
        is org.jetbrains.kotlin.buildtools.internal.serializability.CompilerMessageLocationWithRange -> this.toCompiler()
        else -> this
    }


@Serializable
public data class CompilerMessageLocation(
    override val path: String,
    override val line: Int,
    override val column: Int,
    override val lineContent: String?,
) : CompilerMessageSourceLocation {
    public constructor(from: CompilerMessageLocation) : this(from.path, from.line, from.column, from.lineContent)

    public fun toCompiler(): CompilerMessageLocation? = CompilerMessageLocation.create(path, line, column, lineContent)
}

@Serializable
public data class CompilerMessageLocationWithRange(
    override val path: String,
    override val line: Int,
    override val column: Int,
    override val lineEnd: Int,
    override val columnEnd: Int,
    override val lineContent: String?,
) : CompilerMessageSourceLocation {
    public constructor(from: CompilerMessageLocationWithRange) : this(
        from.path,
        from.line,
        from.column,
        from.lineEnd,
        from.columnEnd,
        from.lineContent
    )

    public fun toCompiler(): CompilerMessageLocationWithRange? =
        CompilerMessageLocationWithRange.create(path, line, column, lineEnd, columnEnd, lineContent)
}

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
            is SourcesChanges.Unknown -> SourcesChangesSurrogate.Unknown
            is SourcesChanges.ToBeCalculated -> SourcesChangesSurrogate.ToBeCalculated
            is SourcesChanges.Known -> SourcesChangesSurrogate.Known(
                value.modifiedFiles.map { it.path },
                value.removedFiles.map { it.path }
            )
        }
        encoder.encodeSerializableValue(SourcesChangesSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): SourcesChanges {
        return when (val surrogate = decoder.decodeSerializableValue(SourcesChangesSurrogate.serializer())) {
            is SourcesChangesSurrogate.Unknown -> SourcesChanges.Unknown
            is SourcesChangesSurrogate.ToBeCalculated -> SourcesChanges.ToBeCalculated
            is SourcesChangesSurrogate.Known -> SourcesChanges.Known(
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
    @Serializable(with = ListOfPathsAsStringSerializer::class)
    val dependenciesSnapshotFiles: List<Path>,
    @Serializable(with = PathAsStringSerializer::class)
    val shrunkClasspathSnapshot: Path,
    @SerialName("PRECISE_JAVA_TRACKING")
    val preciseJavaTracking: Boolean = false,
    @SerialName("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES")
    val assuredNoClasspathSnapshotChanges: Boolean = false,
    @SerialName("USE_FIR_RUNNER")
    val useFirRunner: Boolean = false,
    @SerialName("ROOT_PROJECT_DIR")
    @Serializable(with = PathAsStringSerializer::class)
    val rootProjectDir: Path? = null,
    @SerialName("MODULE_BUILD_DIR")
    @Serializable(with = PathAsStringSerializer::class)
    val moduleBuildDir: Path? = null,
    @SerialName("BACKUP_CLASSES")
    val backupClasses: Boolean = false,
    @SerialName("KEEP_IC_CACHES_IN_MEMORY")
    val keepIcCachesInMemory: Boolean = false,
    @SerialName("FORCE_RECOMPILATION")
    val forceRecompilation: Boolean = false,
    @SerialName("OUTPUT_DIRS")
    @Serializable(with = SetOfPathsAsStringSerializer::class)
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

@Serializable
private class IncrementalModuleSurrogate(
    val name: String,
    val output: Path,
    val buildDir: Path,
    val buildHistoryDir: Path? = null,
)

internal object IncrementalModuleSerializer :
    KSerializer<IncrementalModule> {
    override val descriptor: SerialDescriptor =
        IncrementalModuleSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: IncrementalModule) {
        val surrogate = IncrementalModuleSurrogate(
            name = value.name,
            output = value.output,
            buildDir = value.buildDir,
            buildHistoryDir = value.buildHistoryDir,
        )
        encoder.encodeSerializableValue(IncrementalModuleSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): IncrementalModule {
        val surrogate = decoder.decodeSerializableValue(IncrementalModuleSurrogate.serializer())
        return IncrementalModule(
            name = surrogate.name,
            output = surrogate.output,
            buildDir = surrogate.buildDir,
            buildHistoryDir = surrogate.buildHistoryDir,
        )
    }
}

@Serializable
private class WasmIncrementalModuleSurrogate(
    val name: String,
    val output: Path,
    val buildDir: Path,
    val buildHistoryDir: Path? = null,
)

internal object WasmIncrementalModuleSerializer :
    KSerializer<WasmIncrementalModule> {
    override val descriptor: SerialDescriptor =
        WasmIncrementalModuleSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: WasmIncrementalModule) {
        val surrogate = WasmIncrementalModuleSurrogate(
            name = value.name,
            output = value.output,
            buildDir = value.buildDir,
            buildHistoryDir = value.buildHistoryDir,
        )
        encoder.encodeSerializableValue(WasmIncrementalModuleSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): WasmIncrementalModule {
        val surrogate = decoder.decodeSerializableValue(WasmIncrementalModuleSurrogate.serializer())
        return WasmIncrementalModule(
            name = surrogate.name,
            output = surrogate.output,
            buildDir = surrogate.buildDir,
            buildHistoryDir = surrogate.buildHistoryDir,
        )
    }
}
