/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.decompiler

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION

object KotlinNativeMetaFileType : FileType {
    override fun getName() = "KNM"
    override fun getDescription() = "Kotlin/Native Metadata"
    override fun getDefaultExtension() = KLIB_METADATA_FILE_EXTENSION
    override fun getIcon(): Nothing? = null
    override fun isBinary() = true
    override fun isReadOnly() = true
    override fun getCharset(file: VirtualFile, content: ByteArray): Nothing? = null

    const val STUB_VERSION = 2
}
