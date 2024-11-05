/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.lines

import org.jetbrains.kotlin.backend.wasm.dwarf.StringTable
import org.jetbrains.kotlin.backend.wasm.dwarf.utils.IndexedSet
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class FileTable : Iterable<FileTable.FileInfo> {
    private val set = IndexedSet<FileInfo>()

    val size: Int get() = set.size

    fun add(
        fileName: StringTable.StringRef,
        directory: DirectoryTable.DirectoryId,
    ): FileId = FileId(set.add(FileInfo(fileName, directory)))

    override fun iterator() = set.iterator()

    @JvmInline value class FileId(val index: Int)

    data class FileInfo(
        val path: StringTable.StringRef,
        val directory: DirectoryTable.DirectoryId,
    )
}

