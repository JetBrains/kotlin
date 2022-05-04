/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

internal class KotlinLibraryHeader(library: KotlinLibrary) {
    val sourceFiles: List<KotlinSourceFile>
    val signatureDeserializers: Map<KotlinSourceFile, IdSignatureDeserializer>

    init {
        val filesCount = library.fileCount()
        val extReg = ExtensionRegistryLite.newInstance()
        val files = buildListUntil(filesCount) {
            val fileProto = IrFile.parseFrom(library.file(it).codedInputStream, extReg)
            add(KotlinSourceFile(fileProto.fileEntry.name))
        }

        val deserializers = buildMapUntil(filesCount) {
            put(files[it], IdSignatureDeserializer(IrLibraryFileFromBytes(object : IrLibraryBytesSource() {
                private fun err(): Nothing = icError("Not supported")
                override fun irDeclaration(index: Int): ByteArray = err()
                override fun type(index: Int): ByteArray = err()
                override fun signature(index: Int): ByteArray = library.signature(index, it)
                override fun string(index: Int): ByteArray = library.string(index, it)
                override fun body(index: Int): ByteArray = err()
                override fun debugInfo(index: Int): ByteArray? = null
            }), null))
        }

        sourceFiles = files
        signatureDeserializers = deserializers
    }
}
