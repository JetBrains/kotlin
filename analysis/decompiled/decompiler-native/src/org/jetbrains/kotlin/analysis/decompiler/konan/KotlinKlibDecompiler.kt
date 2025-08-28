/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.compiled.ClsStubBuilder
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider

class KotlinKlibDecompiler : ClassFileDecompilers.Full() {
    private val metadataStubBuilder: KlibMetadataStubBuilder get() = KlibMetadataStubBuilder

    override fun accepts(file: VirtualFile): Boolean = FileTypeRegistry.getInstance().isFileOfType(file, metadataStubBuilder.fileType)

    override fun getStubBuilder(): ClsStubBuilder = metadataStubBuilder

    override fun createFileViewProvider(
        file: VirtualFile,
        manager: PsiManager,
        physical: Boolean,
    ) = KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
        KlibDecompiledFile(provider)
    }
}
