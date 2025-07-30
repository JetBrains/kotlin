/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.incremental.storage.AbstractBasicMap
import org.jetbrains.kotlin.incremental.storage.BasicFileToPathConverter
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import java.io.DataInput
import java.io.DataOutput
import java.io.File

class ConnectedFilesCache(
    workingDir: File,
    icContext: IncrementalCompilationContext,
) : BasicMapsOwner(workingDir) {
    companion object {
        private const val PLUGIN_RELATED_FILES = "plugin-related-files"
    }

    private val alwaysDirtyFiles: ListLikeMap = registerMap(ListLikeMap(PLUGIN_RELATED_FILES.storageFile, icContext))

    fun getPluginRelatedSourceFiles(): List<File> {
        return alwaysDirtyFiles[0].orEmpty()
    }

    fun recordPluginRelatedSourceFiles(files: List<File>) {
        return alwaysDirtyFiles.set(0, files)
    }
}

private class ListLikeMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<Int, List<File>>(
    storageFile,
    keyDescriptor = IntKeyDescriptor,
    valueExternalizer = FileListExternalizer,
    icContext,
)

private object IntKeyDescriptor : KeyDescriptor<Int> {
    override fun getHashCode(value: Int): Int {
        return value
    }

    override fun save(out: DataOutput, value: Int) {
        out.writeInt(value)
    }

    override fun isEqual(val1: Int, val2: Int): Boolean {
        return val1 == val2
    }

    override fun read(`in`: DataInput): Int {
        return `in`.readInt()
    }
}

private object FileListExternalizer : DataExternalizer<List<File>> {
    override fun save(out: DataOutput, value: List<File>) {
        out.writeInt(value.size)
        val fileSerializer = BasicFileToPathConverter.getFileDescriptor()
        for (file in value) {
            fileSerializer.save(out, file)
        }
    }

    override fun read(`in`: DataInput): List<File> {
        val size = `in`.readInt()
        val fileDeserializer = BasicFileToPathConverter.getFileDescriptor()
        return buildList {
            repeat(size) {
                add(fileDeserializer.read(`in`))
            }
        }
    }
}
