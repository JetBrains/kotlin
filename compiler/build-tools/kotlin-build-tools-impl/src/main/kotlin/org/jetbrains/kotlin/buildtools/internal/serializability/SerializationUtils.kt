/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.serializability

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import org.jetbrains.kotlin.buildtools.internal.CompilationStartedEvent
import org.jetbrains.kotlin.buildtools.internal.CompileIterationEvent
import org.jetbrains.kotlin.buildtools.internal.FileCompilationReasonBasic
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import java.nio.file.Path
import kotlin.io.path.Path

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
internal inline fun <T> Any?.cast(): T = this as T

internal fun Any.throwPropertyNotFound(key: String): Nothing = error("Property with name $key not found in $this")

internal object PathAsStringSerializer : KSerializer<Path> {
    override val descriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.toFile().path)

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

internal object IcEventSerializer : KSerializer<IcEvent> {
    override val descriptor: SerialDescriptor = IcEventSurrogate.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: IcEvent,
    ) {
        val surrogate = when (value) {
            is IcEvent.CompilationStarted -> IcEventSurrogate.CompilationStartedSurrogate(
                iteration = value.iteration,
                timestamp = value.timestamp,
                type = value.type,
                isIncremental = value.isIncremental,
                reason = value.reason,
            )
            is IcEvent.CompileIteration -> IcEventSurrogate.CompileIterationSurrogate(
                iteration = value.iteration,
                timestamp = value.timestamp,
                type = value.type,
                reasons = value.reasons.mapValues { entry -> entry.value.map { it.readableString } },
                exitCode = value.exitCode,
            )
            else -> error("Unexpected IC event type: $value")
        }
        encoder.encodeSerializableValue(IcEventSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): IcEvent {
        return when (val surrogate = decoder.decodeSerializableValue(IcEventSurrogate.serializer())) {
            is IcEventSurrogate.CompilationStartedSurrogate -> CompilationStartedEvent(
                isIncremental = surrogate.isIncremental,
                reason = surrogate.reason,
                type = surrogate.type,
                timestamp = surrogate.timestamp,
                iteration = surrogate.iteration,
            )
            is IcEventSurrogate.CompileIterationSurrogate -> CompileIterationEvent(
                reasons = surrogate.reasons.mapValues { entry ->
                    entry.value.map { FileCompilationReasonBasic(it) }
                },
                exitCode = surrogate.exitCode,
                type = surrogate.type,
                timestamp = surrogate.timestamp,
                iteration = surrogate.iteration,
            )
        }
    }
}

@Serializable
internal sealed interface IcEventSurrogate {

    val iteration: Int
    val timestamp: Long
    val type: String

    @Serializable
    data class CompilationStartedSurrogate(
        override val iteration: Int,
        override val timestamp: Long,
        override val type: String = "CompilationStarted",
        val isIncremental: Boolean,
        val reason: String?,
    ) : IcEventSurrogate {
        constructor(from: IcEvent.CompilationStarted) : this(
            from.iteration,
            from.timestamp,
            from.type,
            from.isIncremental,
            from.reason,
        )
    }

    @Serializable
    data class CompileIterationSurrogate(
        override val iteration: Int,
        override val timestamp: Long,
        override val type: String = "CompileIteration",
        val reasons: Map<String, List<String>>,
        val exitCode: String,
    ) : IcEventSurrogate {
        constructor(from: IcEvent.CompileIteration) : this(
            from.iteration,
            from.timestamp,
            from.type,
            from.reasons.mapValues { entry -> entry.value.map { it.readableString } },
            from.exitCode,
        )
    }
}

