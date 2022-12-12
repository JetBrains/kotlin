/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder.Result.KotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.ReusableByteArray
import org.jetbrains.kotlin.utils.readToReusableByteArrayRef
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

    override val containingLibrary: String?
        get() = file.path.split("!/").firstOrNull()

    override fun getFileContentsRef(): ReusableByteArray {
        try {
            return file.inputStream.readToReusableByteArrayRef(file.length)
        } catch (e: IOException) {
            throw logFileReadingErrorMessage(e, file)
        }
    }

    override fun equals(other: Any?) = other is VirtualFileKotlinClass && other.file == file
    override fun hashCode() = file.hashCode()
    override fun toString() = "${this::class.java.simpleName}: $file"

    companion object Factory {
        private val LOG = Logger.getInstance(VirtualFileKotlinClass::class.java)
        private val perfCounter = PerformanceCounter.create("Binary class from Kotlin file")

        /**
         * Creates kotlin class. Calls [ReusableByteArray.addRef] is the content was stored inside the result.
         * So, result's reference must be released when done working with it.
         */
        internal fun create(file: VirtualFile, fileContent: ReusableByteArray?): KotlinClassFinder.Result? {
            return perfCounter.time {
                assert(file.extension == JavaClassFileType.INSTANCE.defaultExtension || file.fileType == JavaClassFileType.INSTANCE) { "Trying to read binary data from a non-class file $file" }

                try {
                    val byteContentRef = fileContent?.also { it.addRef() }
                        ?: file.inputStream.readToReusableByteArrayRef(file.length)
                    if (byteContentRef.isNotEmpty()) {
                        val kotlinJvmBinaryClass = create(byteContentRef) { name, classVersion, header, innerClasses ->
                            VirtualFileKotlinClass(file, name, classVersion, header, innerClasses)
                        }

                        return@time kotlinJvmBinaryClass?.let { KotlinClass(it, byteContentRef) }
                            ?: KotlinClassFinder.Result.ClassFileContent(byteContentRef)
                    } else {
                        byteContentRef.release()
                    }
                } catch (e: FileNotFoundException) {
                    // Valid situation. User can delete jar file.
                } catch (e: Throwable) {
                    if (e is ControlFlowException) throw e
                    throw logFileReadingErrorMessage(e, file)
                }
                null
            }
        }

        private fun logFileReadingErrorMessage(e: Throwable, file: VirtualFile): Throwable {
            val errorMessage = renderFileReadingErrorMessage(file)
            LOG.warn(errorMessage, e)
            return IllegalStateException(errorMessage, e)
        }

        private fun renderFileReadingErrorMessage(file: VirtualFile): String =
            "Could not read file: ${file.path}; size in bytes: ${file.length}; file type: ${file.fileType.name}"
    }
}
