/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.incremental.snapshots

import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.PathStringDescriptor
import java.io.File
import java.util.*

class FileSnapshotMap(storageFile: File) : BasicStringMap<FileSnapshot>(storageFile, PathStringDescriptor, FileSnapshotExternalizer) {
    override fun dumpValue(value: FileSnapshot): String =
        value.toString()

    fun compareAndUpdate(newFiles: Iterable<File>): ChangedFiles.Known {
        val snapshotProvider = SimpleFileSnapshotProviderImpl()
        val newOrModified = ArrayList<File>()
        val removed = ArrayList<File>()

        val newPaths = newFiles.mapTo(HashSet()) { it.canonicalPath }
        for (oldPath in storage.keys) {
            if (oldPath !in newPaths) {
                storage.remove(oldPath)
                removed.add(File(oldPath))
            }
        }

        for (path in newPaths) {
            val file = File(path)
            val oldSnapshot = storage[path]
            val newSnapshot = snapshotProvider[file]

            if (oldSnapshot == null || oldSnapshot != newSnapshot) {
                newOrModified.add(file)
                storage[path] = newSnapshot
            }
        }

        return ChangedFiles.Known(newOrModified, removed)
    }
}