/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.psi.text.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.js.DynamicTypeDeserializer

internal open class Fe10KlibMetadataStubBuilder(
    version: Int,
    fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    readFile: (VirtualFile) -> FileWithMetadata?
) : KlibMetadataStubBuilder(version, fileType, readFile) {
    private val renderer: DescriptorRenderer = DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }

    override fun buildMetadataFileStub(fileWithMetadata: FileWithMetadata.Compatible, fileContent: FileContent): PsiFileStub<*> {
        val ktFileText = decompiledText(fileWithMetadata, serializerProtocol(), DynamicTypeDeserializer, renderer)
        return createFileStub(fileContent.project, ktFileText.text)
    }

    private fun createFileStub(project: Project, text: String): PsiFileStub<*> {
        val virtualFile = LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, text)
        virtualFile.language = KotlinLanguage.INSTANCE
        SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile)

        val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
        val file = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, false, false)!!
        return KtFileElementType.INSTANCE.builder.buildStubTree(file) as PsiFileStub<*>
    }
}
