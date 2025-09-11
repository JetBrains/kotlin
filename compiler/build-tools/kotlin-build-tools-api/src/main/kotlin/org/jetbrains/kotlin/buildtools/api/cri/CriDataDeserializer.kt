/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

public interface CriDataDeserializer {
    public fun deserializeLookupData(data: ByteArray): Collection<LookupEntry>
    public fun deserializeFileIdToPathData(data: ByteArray): Collection<FileIdToPathEntry>
    public fun deserializeSubtypeData(data: ByteArray): Collection<SubtypeEntry>
}
