/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName

/**
 * Something capable of serializing the metadata of a source file to a protobuf message, one file at a time.
 */
interface KlibSingleFileMetadataSerializer<SourceFile> {

    /**
     * The number of source files whose metadata is to be serialized.
     */
    val numberOfSourceFiles: Int

    /**
     * Serializes the metadata of a single source file to a protobuf message and returns the message.
     */
    fun serializeSingleFileMetadata(file: SourceFile): ProtoBuf.PackageFragment

    /**
     * Iterates through each file whose metadata is to be serialized, providing an opportunity to call [serializeSingleFileMetadata]
     * and perform additional processing of the serialized data.
     *
     * @param block A closure that accepts the index of the file in the list of source files, the source file, its corresponding
     *   [KtSourceFile], and the fully qualified name of the package containing the file.
     */
    fun forEachFile(block: (Int, SourceFile, KtSourceFile, FqName) -> Unit)
}