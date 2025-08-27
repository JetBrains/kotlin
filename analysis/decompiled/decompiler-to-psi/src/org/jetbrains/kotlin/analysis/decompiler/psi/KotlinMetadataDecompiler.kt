/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import java.io.IOException

abstract class KotlinMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val expectedBinaryVersion: () -> V,
    stubVersion: Int,
) : ClassFileDecompilers.Full() {
    protected open val metadataStubBuilder: KotlinMetadataStubBuilder = KotlinMetadataStubBuilder(
        stubVersion = stubVersion,
        fileType = fileType,
        serializerProtocol = serializerProtocol,
        readFile = ::readFileSafely,
        expectedBinaryVersion = expectedBinaryVersion,
    )

    abstract fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata?

    override fun accepts(file: VirtualFile) = file.extension == fileType.defaultExtension || file.fileType == fileType

    override fun getStubBuilder() = metadataStubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
            val virtualFile = provider.virtualFile
            if (readFileSafely(virtualFile) != null) {
                KtDecompiledFile(provider)
            } else {
                null
            }
        }
    }

    protected fun readFileSafely(file: VirtualFile, content: ByteArray? = null): KotlinMetadataStubBuilder.FileWithMetadata? {
        if (!file.isValid) return null

        return try {
            readFile(content ?: file.contentsToByteArray(false), file)
        } catch (e: IOException) {
            // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
            // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
            // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
            // it's not always allowed and also is likely to degrade performance
            null
        }
    }
}
