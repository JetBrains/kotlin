/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.cri.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.cri.FileIdToPathEntry
import org.jetbrains.kotlin.buildtools.api.cri.LookupEntry
import org.jetbrains.kotlin.buildtools.api.cri.SubtypeEntry

@OptIn(ExperimentalSerializationApi::class, ExperimentalBuildToolsApi::class)
public class CriDataDeserializerImpl {
    public fun deserializeLookupData(data: ByteArray): Collection<LookupEntry> {
        return ProtoBuf.decodeFromByteArray<Collection<LookupEntryImpl>>(data)
    }

    public fun deserializeFileIdToPathData(data: ByteArray): Collection<FileIdToPathEntry> {
        return ProtoBuf.decodeFromByteArray<Collection<FileIdToPathEntryImpl>>(data)
    }

    public fun deserializeSubtypeData(data: ByteArray): Collection<SubtypeEntry> {
        return ProtoBuf.decodeFromByteArray<Collection<SubtypeEntry>>(data)
    }
}
