/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinLabelProviderService
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.psi.KtPlatformInterface
import javax.swing.Icon

/**
 * The file type of KLIB metadata files (`.knm`), the binary declaration format of Kotlin/Native and other KLIB-based targets.
 */
object KlibMetaFileType : FileType {
    override fun getName(): String = "KNM"

    @OptIn(KtPlatformInterface::class)
    override fun getDescription(): String {
        return KotlinLabelProviderService.getService()?.getLabelForKlibMetaFileType()
            ?: DEFAULT_DESCRIPTION
    }

    override fun getDefaultExtension(): String = KLIB_METADATA_FILE_EXTENSION
    override fun getIcon(): Icon? = null
    override fun isBinary(): Boolean = true
    override fun isReadOnly(): Boolean = true
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    private const val DEFAULT_DESCRIPTION = "Klib Metadata"
}
