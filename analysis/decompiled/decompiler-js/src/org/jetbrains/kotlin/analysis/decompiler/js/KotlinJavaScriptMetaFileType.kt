/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.js

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinLabelProviderService
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil

object KotlinJavaScriptMetaFileType : FileType {
    override fun getName() = "KJSM"
    override fun getDescription() = KotlinLabelProviderService.getService()?.getLabelForKotlinJavaScriptMetaFileType()
        ?: "Kotlin JavaScript meta file"

    override fun getDefaultExtension() = KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION
    override fun getIcon() = null
    override fun isBinary() = true
    override fun isReadOnly() = true
    override fun getCharset(file: VirtualFile, content: ByteArray) = null
}
