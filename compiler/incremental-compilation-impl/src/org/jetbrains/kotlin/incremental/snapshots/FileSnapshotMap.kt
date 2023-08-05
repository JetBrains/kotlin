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
import org.jetbrains.kotlin.incremental.storage.BasicMap
import org.jetbrains.kotlin.incremental.storage.FilePathDescriptor
import org.jetbrains.kotlin.incremental.storage.LazyStorageWrapper
import org.jetbrains.kotlin.incremental.storage.createLazyStorage
import java.io.File

class FileSnapshotMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : LazyStorageWrapper<File, FileSnapshot, String, FileSnapshot>(
    storage = createLazyStorage(storageFile, FilePathDescriptor, FileSnapshotExternalizer, icContext),
    publicToInternalKey = icContext.pathConverterForSourceFiles::toPath,
    internalToPublicKey = icContext.pathConverterForSourceFiles::toFile,
    publicToInternalValue = { it },
    internalToPublicValue = { it }
), BasicMap<File, FileSnapshot> {

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

        for (newFile in newFiles) {
            val oldSnapshot = this[newFile]
            val newSnapshot = snapshotProvider[newFile]

            if (oldSnapshot == null || oldSnapshot != newSnapshot) {
                newOrModified.add(newFile)
                this[newFile] = newSnapshot
            }
        }

        return ChangedFiles.Known(newOrModified, removed)
    }
}