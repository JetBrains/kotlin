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

package org.jetbrains.kotlin.incremental

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.incremental.snapshots.FileSnapshotMap
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.incremental.storage.StringCollectionExternalizer
import org.jetbrains.kotlin.modules.TargetId
import java.io.File

class GradleIncrementalCacheImpl(
        targetDataRoot: File,
        targetOutputDir: File?,
        target: TargetId,
        private val reporter: ICReporter
) : IncrementalCacheImpl<TargetId>(targetDataRoot, targetOutputDir, target) {
    companion object {
        private val SOURCE_TO_OUTPUT_FILES = "source-to-output"
        private val SOURCE_SNAPSHOTS = "source-snapshot"
    }

    internal val sourceToOutputMap = registerMap(SourceToOutputFilesMap(SOURCE_TO_OUTPUT_FILES.storageFile))
    internal val sourceSnapshotMap = registerMap(FileSnapshotMap(SOURCE_SNAPSHOTS.storageFile))

    // generatedFiles can contain multiple entries with the same source file
    // for example Kapt3 IC will generate a .java stub and .class stub for each source file
    fun registerOutputForSourceFiles(generatedFiles: List<GeneratedFile<*>>) {
        val sourceToOutput = MultiMap<File, File>()

        for (generatedFile in generatedFiles) {
            for (source in generatedFile.sourceFiles) {
                sourceToOutput.putValue(source, generatedFile.outputFile)
            }
        }

        for ((source, outputs) in sourceToOutput.entrySet()) {
            sourceToOutputMap[source] = outputs
        }
    }

    fun removeOutputForSourceFiles(sources: Iterable<File>) {
        sources.forEach { sourceToOutputMap.remove(it) }
    }

    inner class SourceToOutputFilesMap(storageFile: File) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {
        operator fun set(sourceFile: File, outputFiles: Collection<File>) {
            storage[sourceFile.absolutePath] = outputFiles.map { it.absolutePath }
        }

        operator fun get(sourceFile: File): Collection<File> =
                storage[sourceFile.absolutePath].orEmpty().map(::File)

        override fun dumpValue(value: Collection<String>) = value.dumpCollection()

        fun remove(file: File) {
            // TODO: do it in the code that uses cache, since cache should not generally delete anything outside of it!
            // but for a moment it is an easiest solution to implement
            get(file).forEach {
                reporter.report { "Deleting $it on clearing cache for $file" }
                it.delete()
            }
            storage.remove(file.absolutePath)
        }
    }
}