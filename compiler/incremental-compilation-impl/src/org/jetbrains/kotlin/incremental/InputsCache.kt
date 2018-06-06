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
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.storage.BasicStringMap
import org.jetbrains.kotlin.incremental.storage.PathStringDescriptor
import org.jetbrains.kotlin.incremental.storage.StringCollectionExternalizer
import java.io.File
import java.util.*
import kotlin.collections.HashSet

class InputsCache(
        workingDir: File,
        private val reporter: ICReporter
) : BasicMapsOwner(workingDir) {
    companion object {
        private val SOURCE_SNAPSHOTS = "source-snapshot"
        private val SOURCE_TO_OUTPUT_FILES = "source-to-output"
        private val COMPLEMENTARY_FILES = "complementary-files"
    }

    internal val sourceSnapshotMap = registerMap(FileSnapshotMap(SOURCE_SNAPSHOTS.storageFile))
    private val sourceToOutputMap = registerMap(FilesMap(SOURCE_TO_OUTPUT_FILES.storageFile))
    /**
     * A file X is a complementary to a file Y if they contain corresponding expect/actual declarations.
     * Complementary files should be compiled together during IC so the compiler does not complain
     * about missing parts.
     * TODO: provide a better solution (maintain an index of expect/actual declarations akin to IncrementalPackagePartProvider)
     */
    private val complementaryFilesMap = registerMap(FilesMap(COMPLEMENTARY_FILES.storageFile))

    fun clearComplementaryFilesMapping(dirtyFiles: Collection<File>): Collection<File> {
        val complementaryFiles = HashSet<File>()
        val filesQueue = ArrayDeque(dirtyFiles)
        while (filesQueue.isNotEmpty()) {
            val file = filesQueue.pollFirst()
            complementaryFilesMap.remove(file).filterTo(filesQueue) { complementaryFiles.add(it) }
        }
        complementaryFiles.removeAll(dirtyFiles)
        return complementaryFiles
    }

    internal fun registerComplementaryFiles(expectActualTracker: ExpectActualTrackerImpl) {
        val actualToExpect = hashMapOf<File, MutableSet<File>>()
        for ((expect, actuals) in expectActualTracker.expectToActualMap) {
            for (actual in actuals) {
                actualToExpect.getOrPut(actual) { hashSetOf() }.add(expect)
            }
            complementaryFilesMap[expect] = actuals
        }

        for ((actual, expects) in actualToExpect) {
            complementaryFilesMap[actual] = expects
        }
    }

    fun removeOutputForSourceFiles(sources: Iterable<File>) {
        for (sourceFile in sources) {
            sourceToOutputMap.remove(sourceFile).forEach { it ->
                reporter.report { "Deleting $it on clearing cache for $sourceFile" }
                it.delete()
            }
        }
    }

    // generatedFiles can contain multiple entries with the same source file
    // for example Kapt3 IC will generate a .java stub and .class stub for each source file
    fun registerOutputForSourceFiles(generatedFiles: List<GeneratedFile>) {
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
}

private class FilesMap(storageFile: File)
    : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {

    operator fun set(sourceFile: File, outputFiles: Collection<File>) {
        storage[sourceFile.absolutePath] = outputFiles.map { it.absolutePath }
    }

    operator fun get(sourceFile: File): Collection<File> =
            storage[sourceFile.absolutePath].orEmpty().map(::File)

    override fun dumpValue(value: Collection<String>) =
            value.dumpCollection()

    fun remove(file: File): Collection<File> =
            get(file).also { storage.remove(file.absolutePath) }
}