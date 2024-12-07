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

package org.jetbrains.kotlin.incremental.storage

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.ExternalIntegerKeyDescriptor
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import java.io.DataInput
import java.io.DataOutput
import java.io.File

internal class IdToFileMap(
    storageFile: File,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<Int, File>(
    storageFile,
    LegacyIntExternalizer.toDescriptor(),
    LegacyFileExternalizer(icContext.pathConverterForSourceFiles),
    icContext
)

/** Similar to [LegacyFileExternalizer]. */
private val LegacyIntExternalizer = ExternalIntegerKeyDescriptor.INSTANCE

/**
 * Use [LegacyFqNameExternalizer] instead of [org.jetbrains.kotlin.incremental.IncrementalCompilationContext.fileDescriptorForOutputFiles]
 * for [IdToFileMap] for now because they internally use different types of `DataExternalizer<String>`, and the `compiler-reference-index`
 * module in the Kotlin IDEA plugin currently can only read the old data format (see KTIJ-27258).
 *
 * Once we fix that bug, we can remove this class and use
 * [org.jetbrains.kotlin.incremental.IncrementalCompilationContext.fileDescriptorForOutputFiles].
 */
private class LegacyFileExternalizer(private val pathConverter: FileToPathConverter) : DataExternalizer<File> {

    override fun save(output: DataOutput, file: File) {
        EnumeratorStringDescriptor.INSTANCE.save(output, pathConverter.toPath(file))
    }

    override fun read(input: DataInput): File {
        return pathConverter.toFile(EnumeratorStringDescriptor.INSTANCE.read(input))
    }
}
