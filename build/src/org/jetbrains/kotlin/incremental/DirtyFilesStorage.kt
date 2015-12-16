/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.LongExternalizer
import java.io.File
import java.util.*

class DirtyFilesStorage(private val basePath: File, private val targetDataDir: File) : BasicMapsOwner() {

    private val String.storageFile: File
        get() = File(targetDataDir, this + "." + CACHE_EXTENSION)

    private val dirtyFilesModifiedMap = registerMap(DirtyFilesMap("dirtyFiles".storageFile))

    private fun normalizePath(file: File): String = file.relativeTo(basePath)

    fun add(file: File, date: Date = Date(file.lastModified())) {
        dirtyFilesModifiedMap.add(normalizePath(file), date)
    }

    fun get(file: File): Date? = dirtyFilesModifiedMap.get(normalizePath(file))

    fun pop(file: File): Date? = dirtyFilesModifiedMap.pop(normalizePath(file))

    private inner class DirtyFilesMap(storageFile: File) : BasicStringMap<Long>(storageFile, LongExternalizer) {

        fun add(path: String, date: Date) {
            storage[path] = date.time
        }

        fun get(path: String): Date? = storage[path]?.let { Date(it) }

        fun pop(path: String): Date? = storage[path]?.let {
            storage.remove(path)
            Date(it)
        }

        fun remove(path: String) {
            storage.remove(path)
        }

        override fun dumpValue(value: Long): String = value.toString()
    }
}


fun DirtyFilesStorage.popAll(files: Iterable<File>): Iterable<Pair<File, Date>> = files.mapNotNull { file -> pop(file)?.let { Pair(file, it) } }

fun DirtyFilesStorage.popAllFiles(files: Iterable<File>): Iterable<File> = files.filter { pop(it) != null }
