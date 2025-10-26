/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.cri.FileIdToPathEntry
import org.jetbrains.kotlin.buildtools.api.cri.LookupEntry
import org.jetbrains.kotlin.buildtools.api.cri.SubtypeEntry
import java.io.DataInputStream

@OptIn(ExperimentalSerializationApi::class, ExperimentalBuildToolsApi::class)
public class CriDataDeserializerImpl {
    public fun deserializeLookupData(data: ByteArray): Iterable<LookupEntry> {
        return decodeFromByteArrayWithLengthPrefix<LookupEntryImpl>(data)
    }

    public fun deserializeFileIdToPathData(data: ByteArray): Iterable<FileIdToPathEntry> {
        return decodeFromByteArrayWithLengthPrefix<FileIdToPathEntryImpl>(data)
    }

    public fun deserializeSubtypeData(data: ByteArray): Iterable<SubtypeEntry> {
        return decodeFromByteArrayWithLengthPrefix<SubtypeEntryImpl>(data)
    }

    private inline fun <reified T> decodeFromByteArrayWithLengthPrefix(data: ByteArray): Iterable<T> {
        return DataInputStream(data.inputStream()).use { stream ->
            generateSequence {
                if (stream.available() < 4) return@generateSequence null
                val size = stream.readInt()
                val bytes = ByteArray(size)
                stream.readFully(bytes)
                ProtoBuf.decodeFromByteArray<T>(bytes)
            }
        }.asIterable()
    }
}
