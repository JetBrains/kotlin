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

package org.jetbrains.kotlin.load.kotlin

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder.Result.KotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.rethrow
import java.io.FileNotFoundException
import java.io.IOException

class VirtualFileKotlinClass private constructor(
        val file: VirtualFile,
        className: ClassId,
        classVersion: Int,
        classHeader: KotlinClassHeader,
        innerClasses: InnerClassesInfo
) : FileBasedKotlinClass(className, classVersion, classHeader, innerClasses) {

    override val location: String
        get() = file.path

    override fun getFileContents(): ByteArray {
        try {
            return file.contentsToByteArray()
        }
        catch (e: IOException) {
            LOG.error(renderFileReadingErrorMessage(file), e)
            throw rethrow(e)
        }
    }

    override fun equals(other: Any?) = other is VirtualFileKotlinClass && other.file == file
    override fun hashCode() = file.hashCode()
    override fun toString() = "${this::class.java.simpleName}: $file"

    companion object Factory {
        private val LOG = Logger.getInstance(VirtualFileKotlinClass::class.java)
        private val perfCounter = PerformanceCounter.create("Binary class from Kotlin file")

        @Deprecated("Use KotlinBinaryClassCache")
        fun create(file: VirtualFile, fileContent: ByteArray?): KotlinClassFinder.Result? {
            return perfCounter.time {
                assert(file.fileType == JavaClassFileType.INSTANCE) { "Trying to read binary data from a non-class file $file" }

                try {
                    val byteContent = fileContent ?: file.contentsToByteArray(false)
                    if (!byteContent.isEmpty()) {
                        val kotlinJvmBinaryClass = FileBasedKotlinClass.create(byteContent) { name, classVersion, header, innerClasses ->
                            VirtualFileKotlinClass(file, name, classVersion, header, innerClasses)
                        }

                        return@time kotlinJvmBinaryClass?.let { KotlinClass(it, byteContent) }
                            ?: KotlinClassFinder.Result.ClassFileContent(byteContent)
                    }
                }
                catch (e: FileNotFoundException) {
                    // Valid situation. User can delete jar file.
                }
                catch (e: Throwable) {
                    LOG.warn(renderFileReadingErrorMessage(file))
                }
                null
            }
        }

        private fun renderFileReadingErrorMessage(file: VirtualFile): String =
                "Could not read file: ${file.path}; size in bytes: ${file.length}; file type: ${file.fileType.name}"
    }
}
