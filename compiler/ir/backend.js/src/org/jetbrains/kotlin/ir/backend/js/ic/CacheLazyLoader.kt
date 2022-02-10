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
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite


class CacheLazyLoader(private val cacheProvider: PersistentCacheProvider, private val library: KotlinLibrary) {
    private val graphInlineCache = mutableMapOf<String, Collection<Pair<IdSignature, TransHash>>>()
    private val fingerPrintCache = mutableMapOf<String, Hash>()

    val signatureReadersList: List<Pair<String, IdSignatureDeserializer>> by lazy {
        library.filesAndSigReaders()
    }

    private val signatureReadersMap: Map<String, IdSignatureDeserializer> by lazy {
        signatureReadersList.toMap()
    }

    val allInlineFunctionHashes: Map<IdSignature, TransHash> by lazy {
        cacheProvider.allInlineHashes { librarySrc, index ->
            val libReader = signatureReadersMap[librarySrc] ?: error("No module reader for lib $librarySrc")
            libReader.deserializeIdSignature(index)
        }
    }

    fun getInlineHashesByFile() = signatureReadersList.associateTo(mutableMapOf()) {
        it.first to cacheProvider.inlineHashes(it.first) { index -> it.second.deserializeIdSignature(index) }
    }

    fun getInlineGraphForFile(srcFile: String) = graphInlineCache.getOrPut(srcFile) {
        val fileReader = signatureReadersMap[srcFile] ?: error("Cannot find signature reader for $srcFile")
        cacheProvider.inlineGraphForFile(srcFile) { index -> fileReader.deserializeIdSignature(index) }
    }

    fun getFileFingerPrint(srcFile: String) = fingerPrintCache.getOrPut(srcFile) {
        cacheProvider.fileFingerPrint(srcFile)
    }

    fun getFilePaths() = cacheProvider.filePaths()

    fun clearCaches() {
        graphInlineCache.clear()
        fingerPrintCache.clear()
    }

    private fun KotlinLibrary.filesAndSigReaders(): List<Pair<String, IdSignatureDeserializer>> {
        val fileSize = fileCount()
        val result = ArrayList<Pair<String, IdSignatureDeserializer>>(fileSize)
        val extReg = ExtensionRegistryLite.newInstance()

        for (i in 0 until fileSize) {
            val fileStream = file(i).codedInputStream
            val fileProto = IrFile.parseFrom(fileStream, extReg)
            val sigReader = IdSignatureDeserializer(IrLibraryFileFromBytes(object : IrLibraryBytesSource() {
                private fun err(): Nothing = error("Not supported")
                override fun irDeclaration(index: Int): ByteArray = err()

                override fun type(index: Int): ByteArray = err()

                override fun signature(index: Int): ByteArray = signature(index, i)

                override fun string(index: Int): ByteArray = string(index, i)

                override fun body(index: Int): ByteArray = err()

                override fun debugInfo(index: Int): ByteArray? = null
            }), null)

            result.add(fileProto.fileEntry.name to sigReader)
        }

        return result
    }
}

