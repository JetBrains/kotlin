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
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.storage.AbstractBasicMap
import java.io.File

class FileSnapshotMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<File, FileSnapshot>(
    storageFile,
    icContext.fileDescriptorForSourceFiles,
    FileSnapshotExternalizer,
    icContext
) {
    @Synchronized
    fun compareAndUpdate(newFiles: Iterable<File>): ChangedFiles.Known {
        val snapshotProvider = SimpleFileSnapshotProviderImpl()
        val newOrModified = ArrayList<File>()
        val removed = ArrayList<File>()

        val newFilesSet = newFiles.toSet()
        for (oldFile in keys) {
            if (oldFile !in newFilesSet) {
                remove(oldFile)
                removed.add(oldFile)
            }
        }

        for (file in newFilesSet) {
            val oldSnapshot = this[file]
            val newSnapshot = snapshotProvider[file]

            if (oldSnapshot == null || oldSnapshot != newSnapshot) {
                newOrModified.add(file)
                this[file] = newSnapshot
            }
        }

        return ChangedFiles.Known(newOrModified, removed)
    }
}