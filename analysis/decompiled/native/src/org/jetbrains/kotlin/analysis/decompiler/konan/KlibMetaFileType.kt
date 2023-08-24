/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinLabelProviderService
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION

object KlibMetaFileType : FileType {
    override fun getName() = "KNM"

    override fun getDescription(): String {
        return KotlinLabelProviderService.getService()?.getLabelForKlibMetaFileType()
            ?: DEFAULT_DESCRIPTION
    }

    override fun getDefaultExtension() = KLIB_METADATA_FILE_EXTENSION
    override fun getIcon(): Nothing? = null
    override fun isBinary() = true
    override fun isReadOnly() = true
    override fun getCharset(file: VirtualFile, content: ByteArray): Nothing? = null

    const val STUB_VERSION = 2
    private const val DEFAULT_DESCRIPTION = "Klib Metadata"
}