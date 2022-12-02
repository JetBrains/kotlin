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
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.util.PerformanceCounter
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

    override fun getFileContents(): ByteArray {
        try {
            return file.contentsToByteArray()
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

        internal fun create(file: VirtualFile, jvmMetadataVersion: JvmMetadataVersion, fileContent: ByteArray?): KotlinClassFinder.Result? {
            return perfCounter.time {
                assert(file.extension == JavaClassFileType.INSTANCE.defaultExtension || file.fileType == JavaClassFileType.INSTANCE) { "Trying to read binary data from a non-class file $file" }

                try {
                    val byteContent = fileContent ?: file.contentsToByteArray(false)
                    if (byteContent.isNotEmpty()) {
                        val kotlinJvmBinaryClass = create(byteContent, jvmMetadataVersion) { name, classVersion, header, innerClasses ->
                            VirtualFileKotlinClass(file, name, classVersion, header, innerClasses)
                        }

                        return@time kotlinJvmBinaryClass?.let { KotlinClass(it, byteContent) }
                            ?: KotlinClassFinder.Result.ClassFileContent(byteContent)
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
