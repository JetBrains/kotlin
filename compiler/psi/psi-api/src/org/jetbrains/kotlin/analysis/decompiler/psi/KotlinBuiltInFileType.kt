/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinIconProviderService
import org.jetbrains.kotlin.psi.KtPlatformInterface
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import javax.swing.Icon

/**
 * The file type of Kotlin built-in declaration files: `.kotlin_builtins` and `.kotlin_metadata`.
 */
object KotlinBuiltInFileType : FileType {
    override fun getName(): String = "kotlin_builtins"

    @OptIn(KtPlatformInterface::class)
    override fun getDescription(): String =
        KotlinLabelProviderService.getService()?.getLabelForBuiltInFileType()
            ?: DEFAULT_DESCRIPTION

    override fun getDefaultExtension(): String = BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION

    override fun getIcon(): Icon = KotlinIconProviderService.getInstance().builtInFileIcon

    override fun isBinary(): Boolean = true

    override fun isReadOnly(): Boolean = true

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    private const val DEFAULT_DESCRIPTION = "Kotlin built-in declarations"
}
