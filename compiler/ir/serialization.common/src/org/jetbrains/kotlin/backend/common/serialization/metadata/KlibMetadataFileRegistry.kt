/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.backend.common.serialization.metadata.extractFileId
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.library.metadata.KlibMetadataPackageFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.PsiSourceFile


class KlibMetadataFileRegistry {
    private val fileIdsImpl = mutableMapOf<KlibFileMetadata, Int>()

    fun lookup(file: KlibFileMetadata) = fileIdsImpl.getOrPut(file) { fileIdsImpl.size }

    val fileIds: Map<KlibFileMetadata, Int>
        get() = fileIdsImpl

    fun getFileId(descriptor: DeclarationDescriptor): Int? {
        if (!DescriptorUtils.isTopLevelDeclaration(descriptor) || descriptor !is DeclarationDescriptorWithSource) return null

        val fileId = descriptor.extractFileId()
        if (fileId != null) {
            (descriptor.containingDeclaration as? KlibMetadataPackageFragment)?.let { packageFragment ->
                return this.lookup(KotlinDeserializedFileMetadata(packageFragment, fileId))
            }
        }

        val file = descriptor.source.containingFile as? PsiSourceFile ?: return null

        val psiFile = file.psiFile
        return (psiFile as? KtFile)?.let { this.lookup(KotlinPsiFileMetadata(it)) }
    }

}
