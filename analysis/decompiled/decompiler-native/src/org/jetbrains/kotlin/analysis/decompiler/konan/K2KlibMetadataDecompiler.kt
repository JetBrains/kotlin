/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

abstract class K2KlibMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val stubVersion: Int,
) : KlibMetadataDecompiler<V>(fileType) {
    override val metadataStubBuilder: KlibMetadataStubBuilder by lazy {
        K2KlibMetadataStubBuilder(stubVersion, fileType, serializerProtocol, ::readFileSafely)
    }
}
