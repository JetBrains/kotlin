/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.stub.createIncompatibleAbiVersionFileStub

abstract class KlibMetadataStubBuilder(
    private val version: Int,
    private val fileType: FileType,
    private val readFile: (VirtualFile) -> FileWithMetadata?
) : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + version

    protected abstract fun buildMetadataFileStub(fileWithMetadata: FileWithMetadata.Compatible, fileContent: FileContent): PsiFileStub<*>

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        assert(FileTypeRegistry.getInstance().isFileOfType(virtualFile, fileType)) { "Unexpected file type ${virtualFile.fileType}" }

        val fileWithMetadata = readFile(virtualFile) ?: return null

        return when (fileWithMetadata) {
            is FileWithMetadata.Incompatible -> createIncompatibleAbiVersionFileStub()
            is FileWithMetadata.Compatible -> buildMetadataFileStub(fileWithMetadata, content)
        }
    }
}
