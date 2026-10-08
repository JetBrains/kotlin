/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.InaccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker.ScopeKind
import org.jetbrains.kotlin.buildtools.internal.LogLevel
import org.jetbrains.kotlin.buildtools.internal.arguments.absolutePathStringOrThrow
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.js.operations.JsLinkingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmClasspathSnapshottingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.jvm.operations.JvmCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.metadata.operations.KotlinMetadataKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmLinkingOperationImpl
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties


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

    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.absolutePathStringOrThrow())

    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}

internal object ListOfPathsAsStringSerializer : KSerializer<List<Path>> by ListSerializer(PathAsStringSerializer)

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
//        subclass(JvmCompilationOperationImpl::class)
//        subclass(JvmCompilationOperationImpl::class)
//        subclass(JvmCompilationOperationImpl::class)
    }
    polymorphic(CompilerMessageSourceLocation::class) {
        subclass(org.jetbrains.kotlin.buildtools.internal.serializability.CompilerMessageLocation::class)
        subclass(org.jetbrains.kotlin.buildtools.internal.serializability.CompilerMessageLocationWithRange::class)
    }
//    polymorphic(Messages::class) {
//        subclass(Messages.LogLine::class)
//        subclass(Messages.LookupClear::class)
//        subclass(Messages.LookupMessage::class)
//    }
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
        public val value: Long
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

@Serializer(forClass = JvmSnapshotBasedIncrementalCompilationConfigurationImpl::class)
internal object JvmSnapshotBasedIncrementalCompilationConfigurationImplSerializer
