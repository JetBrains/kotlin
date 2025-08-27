/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.compiled.ClsStubBuilder
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import java.io.IOException

abstract class KlibMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
) : ClassFileDecompilers.Full() {
    protected abstract val metadataStubBuilder: KlibMetadataStubBuilder

    protected fun doReadFile(file: VirtualFile): FileWithMetadata? {
        return FileWithMetadata.forPackageFragment(file)
    }

    override fun accepts(file: VirtualFile): Boolean = FileTypeRegistry.getInstance().isFileOfType(file, fileType)

    override fun getStubBuilder(): ClsStubBuilder = metadataStubBuilder

    override fun createFileViewProvider(
        file: VirtualFile,
        manager: PsiManager,
        physical: Boolean,
    ) = KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
        KlibDecompiledFile(provider)
    }

    protected fun readFileSafely(file: VirtualFile): FileWithMetadata? {
        if (!file.isValid) return null

        return try {
            doReadFile(file)
        } catch (_: IOException) {
            // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
            // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
            // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
            // it's not always allowed and also is likely to degrade performance
            null
        }
    }
}
