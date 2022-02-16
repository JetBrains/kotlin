/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.ir.util.StringSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile


class CacheLazyLoader(private val cacheProvider: PersistentCacheProvider, private val library: KotlinLibrary) {
    private val graphInlineCache = mutableMapOf<String, Collection<Pair<StringSignature, TransHash>>>()
    private val fingerPrintCache = mutableMapOf<String, Hash>()

    val libraryFiles: List<String> by lazy {
        library.files()
    }

    val allInlineFunctionHashes: Map<StringSignature, TransHash> by lazy {
        cacheProvider.allInlineHashes { librarySrc, index ->
            val fileIndex = libraryFiles.indexOf(librarySrc)
            assert(fileIndex >= 0) { "No module reader for file $librarySrc" }
            StringSignature(library.string(index, fileIndex).decodeToString())
        }
    }

    fun getInlineHashesByFile(): MutableMap<String, Map<StringSignature, TransHash>> {
        var index = 0
        return libraryFiles.associateWithTo(mutableMapOf()) { fileName ->
            val fileIndex = index++
            cacheProvider.inlineHashes(fileName) { sigIndex ->
                StringSignature(library.signature(sigIndex, fileIndex).decodeToString())
            }
        }
    }

    fun getInlineGraphForFile(srcFile: String) = graphInlineCache.getOrPut(srcFile) {
        val fileIndex = libraryFiles.indexOf(srcFile)
        assert(fileIndex >= 0)
        cacheProvider.inlineGraphForFile(srcFile) { index -> StringSignature(library.string(index, fileIndex).decodeToString()) }
    }

    fun getFileFingerPrint(srcFile: String) = fingerPrintCache.getOrPut(srcFile) {
        cacheProvider.fileFingerPrint(srcFile)
    }

    fun getFilePaths() = cacheProvider.filePaths()

    fun clearCaches() {
        graphInlineCache.clear()
        fingerPrintCache.clear()
    }

    private fun KotlinLibrary.files(): List<String> {
        val fileSize = fileCount()
        val result = ArrayList<String>(fileSize)
        val extReg = ExtensionRegistryLite.newInstance()

        for (i in 0 until fileSize) {
            val fileStream = file(i).codedInputStream
            val fileProto = ProtoFile.parseFrom(fileStream, extReg)
            result.add(fileProto.fileEntry.name)
        }

        return result
    }
}

