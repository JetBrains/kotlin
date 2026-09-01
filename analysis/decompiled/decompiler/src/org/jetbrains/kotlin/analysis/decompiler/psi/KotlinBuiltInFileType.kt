// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinIconProviderService
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import javax.swing.Icon

object KotlinBuiltInFileType : FileType {
    override fun getName() = "kotlin_builtins"

    override fun getDescription(): String =
        KotlinLabelProviderService.getService()?.getLabelForBuiltInFileType()
            ?: DEFAULT_DESCRIPTION

    override fun getDefaultExtension() = BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION

    override fun getIcon(): Icon = KotlinIconProviderService.getInstance().builtInFileIcon

    override fun isBinary() = true

    override fun isReadOnly() = true

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    private const val DEFAULT_DESCRIPTION = "Kotlin built-in declarations"
}
