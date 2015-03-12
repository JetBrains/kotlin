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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.utils.*

import java.io.IOException
import java.io.FileNotFoundException

public class VirtualFileKotlinClass private(
        public val file: VirtualFile,
        className: ClassId,
        classHeader: KotlinClassHeader,
        innerClasses: FileBasedKotlinClass.InnerClassesInfo
) : FileBasedKotlinClass(className, classHeader, innerClasses) {

    override fun getLocation() = file.getPath()

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
    override fun toString() = "${javaClass.getSimpleName()}: $file"

    default object Factory {
        private val LOG = Logger.getInstance(javaClass<VirtualFileKotlinClass>())

        deprecated("Use KotlinBinaryClassCache")
        fun create(file: VirtualFile): VirtualFileKotlinClass? {
            assert(file.getFileType() == JavaClassFileType.INSTANCE) { "Trying to read binary data from a non-class file $file" }

            try {
                val byteContent = file.contentsToByteArray()
                if (byteContent.isEmpty()) return null

                return FileBasedKotlinClass.create(byteContent) {
                    name, header, innerClasses ->
                    VirtualFileKotlinClass(file, name, header, innerClasses)
                }
            }
            catch (e: FileNotFoundException) {
                // Valid situation. User can delete jar file.
            }
            catch (e: Throwable) {
                LOG.warn(renderFileReadingErrorMessage(file))
            }

            return null
        }

        private fun renderFileReadingErrorMessage(file: VirtualFile): String =
                "Could not read file: ${file.getPath()}; size in bytes: ${file.getLength()}; file type: ${file.getFileType().getName()}"
    }
}
