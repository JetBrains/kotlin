/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.compiled.ClsStubBuilder
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder

abstract class KotlinMetadataDecompiler : ClassFileDecompilers.Full() {
    protected abstract val metadataStubBuilder: KotlinMetadataStubBuilder

    private val fileType: FileType get() = metadataStubBuilder.fileType

    override fun accepts(file: VirtualFile) = file.extension == fileType.defaultExtension || file.fileType == fileType

    override fun getStubBuilder(): ClsStubBuilder = metadataStubBuilder

    protected abstract fun createFile(viewProvider: KotlinDecompiledFileViewProvider): KtDecompiledFile

    final override fun createFileViewProvider(
        file: VirtualFile,
        manager: PsiManager,
        physical: Boolean,
    ): KotlinDecompiledFileViewProvider = KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
        val virtualFile = provider.virtualFile
        if (metadataStubBuilder.readFileSafely(virtualFile) != null) {
            createFile(provider)
        } else {
            null
        }
    }
}
