/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.cri

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.FqName
import kotlin.collections.component1
import kotlin.collections.component2

@OptIn(ExperimentalSerializationApi::class)
public class CriDataSerializerImpl {

    @Suppress("ArrayInDataClass")
    public data class SerializedLookupData(
        val lookups: ByteArray,
        val fileIdsToPaths: ByteArray,
    )

    public fun serializeLookups(lookups: Map<LookupSymbol, Collection<String>>): SerializedLookupData {
        val filePathToId = mutableMapOf<String, Int>()

        fun addFilePathIfNeeded(filePath: String): Int = filePathToId.getOrPut(filePath) {
            (filePathToId.size + 1)
        }

        fun Map.Entry<LookupSymbol, Collection<String>>.toLookupEntry(): LookupEntryImpl = LookupEntryImpl(
            key,
            value.map { addFilePathIfNeeded(it) },
        )

        val lookups = lookups.entries.map { it.toLookupEntry() }
        val fileIdsToPaths = filePathToId.map { (filePath, fileId) -> FileIdToPathEntryImpl(fileId, filePath) }

        return SerializedLookupData(
            lookups = ProtoBuf.encodeToByteArray(lookups),
            fileIdsToPaths = ProtoBuf.encodeToByteArray(fileIdsToPaths),
        )
    }

    public fun serializeSubtypes(subtypes: Map<FqName, Collection<FqName>>): ByteArray {
        val subtypes = subtypes.map { (className, subtypes) -> SubtypeEntryImpl(className, subtypes) }
        return ProtoBuf.encodeToByteArray(subtypes)
    }
}
