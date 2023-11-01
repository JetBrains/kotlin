/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.text.DecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.createIncompatibleAbiVersionDecompiledText
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import java.io.IOException

abstract class KlibMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val flexibleTypeDeserializer: FlexibleTypeDeserializer,
    private val expectedBinaryVersion: () -> V,
    private val invalidBinaryVersion: () -> V
) : ClassFileDecompilers.Full() {
    protected abstract val metadataStubBuilder: KlibMetadataStubBuilder

    protected abstract fun doReadFile(file: VirtualFile): FileWithMetadata?

    protected abstract fun getDecompiledText(
        file: FileWithMetadata.Compatible,
        serializerProtocol: SerializerExtensionProtocol,
        flexibleTypeDeserializer: FlexibleTypeDeserializer
    ): DecompiledText

    override fun accepts(file: VirtualFile) = FileTypeRegistry.getInstance().isFileOfType(file, fileType)

    override fun getStubBuilder() = metadataStubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean) =
        KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
            KlibDecompiledFile(
                provider,
                ::buildDecompiledText
            )
        }

    protected fun readFileSafely(file: VirtualFile): FileWithMetadata? {
        if (!file.isValid) return null

        return try {
            doReadFile(file)
        } catch (e: IOException) {
            // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
            // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
            // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
            // it's not always allowed and also is likely to degrade performance
            null
        }
    }

    private fun buildDecompiledText(virtualFile: VirtualFile): DecompiledText {
        assert(FileTypeRegistry.getInstance().isFileOfType(virtualFile, fileType)) { "Unexpected file type ${virtualFile.fileType}" }

        return when (val file = readFileSafely(virtualFile)) {
            is FileWithMetadata.Incompatible -> createIncompatibleAbiVersionDecompiledText(expectedBinaryVersion(), file.version)
            is FileWithMetadata.Compatible -> getDecompiledText(file, serializerProtocol(), flexibleTypeDeserializer)
            null -> createIncompatibleAbiVersionDecompiledText(expectedBinaryVersion(), invalidBinaryVersion())
        }
    }

    @TestOnly
    internal fun buildDecompiledTextForTests(virtualFile: VirtualFile): DecompiledText = buildDecompiledText(virtualFile)
}