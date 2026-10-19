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
