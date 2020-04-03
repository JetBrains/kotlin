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
import org.jetbrains.kotlin.incremental.storage.SourceToOutputFilesMap
import java.io.File

class InputsCache(
    workingDir: File,
    private val reporter: ICReporter
) : BasicMapsOwner(workingDir) {
    companion object {
        private const val SOURCE_SNAPSHOTS = "source-snapshot"
        private const val SOURCE_TO_OUTPUT_FILES = "source-to-output"
    }

    internal val sourceSnapshotMap = registerMap(FileSnapshotMap(SOURCE_SNAPSHOTS.storageFile))
    private val sourceToOutputMap = registerMap(SourceToOutputFilesMap(SOURCE_TO_OUTPUT_FILES.storageFile))

    fun removeOutputForSourceFiles(sources: Iterable<File>) {
        for (sourceFile in sources) {
            sourceToOutputMap.remove(sourceFile).forEach {
                reporter.reportVerbose { "Deleting $it on clearing cache for $sourceFile" }
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