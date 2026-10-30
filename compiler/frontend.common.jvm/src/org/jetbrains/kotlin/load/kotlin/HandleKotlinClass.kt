/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder.Result.KotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.PhaseSideType
import org.jetbrains.kotlin.util.tryMeasureSideTime
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.NoSuchFileException

/**
 * A Kotlin binary class read from a [BinaryClassFileHandle]: the CLI counterpart of [VirtualFileKotlinClass], which stays
 * for the PSI view, the Analysis API and the decompiler.
 *
 * The class is identified by its [classFile]; [containingLibraryPath] is the library *root* the file was found in, which is
 * what `LibraryPathFilter` matches a library session by.
 */
class HandleKotlinClass private constructor(
    val classFile: BinaryClassFileHandle,
    className: ClassId,
    classVersion: Int,
    classAccess: Int,
    classHeader: KotlinClassHeader,
    innerClasses: InnerClassesInfo,
) : FileBasedKotlinClass(className, classVersion, classAccess, classHeader, innerClasses) {

    override val location: String
        get() = classFile.pathForDiagnostics

    override val containingLibrary: String?
        get() = classFile.pathForDiagnostics.split("!/").firstOrNull()

    override val containingLibraryPath: PathHolder?
        get() = classFile.rootPath?.let(::PathHolder)

    override fun getFileContents(): ByteArray {
        try {
            return classFile.readBytes()
        } catch (e: IOException) {
            throw logFileReadingErrorMessage(e, classFile)
        }
    }

    override fun equals(other: Any?) = other is HandleKotlinClass && other.classFile == classFile
    override fun hashCode() = classFile.hashCode()
    override fun toString() = "${this::class.java.simpleName}: $classFile"

    companion object Factory {
        private val LOG = Logger.getInstance(HandleKotlinClass::class.java)

        /**
         * Reads [classFile] (or [fileContent], when the caller already has it), returning the Kotlin class, or the content of a
         * class file without Kotlin metadata, or `null` for a file which cannot be read anymore (a jar deleted during the compilation).
         */
        fun create(
            classFile: BinaryClassFileHandle,
            metadataVersion: MetadataVersion,
            fileContent: ByteArray?,
            perfManager: PerformanceManager?,
        ): KotlinClassFinder.Result? {
            return perfManager.tryMeasureSideTime(PhaseSideType.BinaryClassFromKotlinFile) {
                try {
                    val byteContent = fileContent ?: classFile.readBytes()
                    if (byteContent.isNotEmpty()) {
                        val kotlinJvmBinaryClass =
                            create(byteContent, metadataVersion) { name, classVersion, classAccess, header, innerClasses ->
                                HandleKotlinClass(classFile, name, classVersion, classAccess, header, innerClasses)
                            }

                        return@tryMeasureSideTime kotlinJvmBinaryClass?.let { KotlinClass(it, byteContent) }
                            ?: KotlinClassFinder.Result.ClassFileContent(byteContent)
                    }
                } catch (_: FileNotFoundException) {
                    // Valid situation. User can delete jar file.
                } catch (_: NoSuchFileException) {
                    // Same (see FL-32618 as an exception example)
                } catch (e: Throwable) {
                    if (e is ControlFlowException) throw e
                    throw logFileReadingErrorMessage(e, classFile)
                }
                null
            }
        }

        private fun logFileReadingErrorMessage(e: Throwable, classFile: BinaryClassFileHandle): Throwable {
            val errorMessage = "Could not read file: ${classFile.pathForDiagnostics}"
            LOG.warn(errorMessage, e)
            return IllegalStateException(errorMessage, e)
        }
    }
}
