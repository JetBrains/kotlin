/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.name.FqName
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.collections.component1
import kotlin.collections.component2

@OptIn(ExperimentalSerializationApi::class)
public class CriDataSerializerImpl {

    @Suppress("ArrayInDataClass")
    public data class SerializedLookupData(
        val lookups: ByteArray,
        val fileIdsToPaths: ByteArray,
    )

    public fun serializeLookups(
        lookups: Map<LookupSymbol, Collection<String>>,
        sourceFilesPathConverter: FileToPathConverter,
    ): SerializedLookupData {
        val filePathToId = mutableMapOf<String, Int>()

        fun addFilePathIfNeeded(filePath: String): Int = filePathToId.getOrPut(
            sourceFilesPathConverter.toPath(sourceFilesPathConverter.toFile(filePath))
        ) { filePath.hashCode() }

        fun Map.Entry<LookupSymbol, Collection<String>>.toLookupEntry(): LookupEntryImpl = LookupEntryImpl(
            key,
            value.map { addFilePathIfNeeded(it) },
        )

        val lookups = lookups.asSequence().map { it.toLookupEntry() }
        val serializedLookups = lookups.encodeToByteArrayWithLengthPrefix()

        val fileIdsToPaths = filePathToId.asSequence().map { (filePath, fileId) ->
            FileIdToPathEntryImpl(fileId, filePath)
        }
        val serializedFileIdsToPaths = fileIdsToPaths.encodeToByteArrayWithLengthPrefix()

        return SerializedLookupData(
            lookups = serializedLookups,
            fileIdsToPaths = serializedFileIdsToPaths,
        )
    }

    public fun serializeSubtypes(subtypes: Map<FqName, Collection<FqName>>): ByteArray {
        val subtypes = subtypes.asSequence().map { (className, subtypes) ->
            SubtypeEntryImpl(className, subtypes)
        }
        return subtypes.encodeToByteArrayWithLengthPrefix()
    }

    private inline fun <reified T> Sequence<T>.encodeToByteArrayWithLengthPrefix(): ByteArray {
        val stream = ByteArrayOutputStream()
        DataOutputStream(stream).use { dataStream ->
            forEach {
                val serializedEntry = ProtoBuf.encodeToByteArray(it)
                dataStream.writeInt(serializedEntry.size)
                dataStream.write(serializedEntry)
            }
        }
        return stream.toByteArray()
    }
}
