/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import org.jetbrains.kotlin.idea.core.util.readNullable
import org.jetbrains.kotlin.idea.core.util.readStringList
import org.jetbrains.kotlin.idea.core.util.writeNullable
import org.jetbrains.kotlin.idea.core.util.writeStringList
import java.io.DataInputStream
import java.io.DataOutput

/**
 * Optimized collection for storing last modified files with ability to
 * get time of last modified file expect given one ([lastModifiedTimeStampExcept]).
 *
 * This is required since Gradle scripts configurations should be updated on
 * each other script changes (but not on the given script changes itself).
 *
 * Actually works by storing two last timestamps with the set of files modified at this times.
 */
class LastModifiedFiles(
    private var last: SimultaneouslyChangedFiles = SimultaneouslyChangedFiles(),
    private var previous: SimultaneouslyChangedFiles = SimultaneouslyChangedFiles()
) {
    init {
        previous.fileIds.removeAll(last.fileIds)
        if (previous.fileIds.isEmpty()) previous = SimultaneouslyChangedFiles()
    }

    class SimultaneouslyChangedFiles(
        val ts: Long = Long.MIN_VALUE,
        val fileIds: MutableSet<String> = mutableSetOf()
    )

    @Synchronized
    fun fileChanged(ts: Long, fileId: String) {
        when {
            ts > last.ts -> {
                val prevPrev = previous
                previous = last
                previous.fileIds.remove(fileId)
                if (previous.fileIds.isEmpty()) previous = prevPrev
                last = SimultaneouslyChangedFiles(ts, hashSetOf(fileId))
            }
            ts == last.ts -> last.fileIds.add(fileId)
            ts == previous.ts -> previous.fileIds.add(fileId)
        }
    }

    @Synchronized
    fun lastModifiedTimeStampExcept(fileId: String): Long = when {
        last.fileIds.size == 1 && last.fileIds.contains(fileId) -> previous.ts
        else -> last.ts
    }

    companion object {
        private val fileAttribute = FileAttribute("last-modified-files", 1, false)

        fun read(buildRoot: VirtualFile): LastModifiedFiles? {
            return fileAttribute.readAttribute(buildRoot)?.use {
                it.readNullable {
                    LastModifiedFiles(readSCF(it), readSCF(it))
                }
            }
        }

        fun write(buildRoot: VirtualFile, data: LastModifiedFiles?) {
            fileAttribute.writeAttribute(buildRoot).use {
                it.writeNullable(data) { data ->
                    writeSCF(data.last)
                    writeSCF(data.previous)
                }
            }
        }

        fun remove(buildRoot: VirtualFile) {
            write(buildRoot, null)
        }

        private fun readSCF(it: DataInputStream) = SimultaneouslyChangedFiles(it.readLong(), it.readStringList().toMutableSet())

        private fun DataOutput.writeSCF(last: SimultaneouslyChangedFiles) {
            writeLong(last.ts)
            writeStringList(last.fileIds.toList())
        }
    }
}