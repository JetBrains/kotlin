/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

interface KtSourceFile {
    val name: String
    val path: String?

    fun getContentsAsStream(): InputStream
}

class KtPsiSourceFile(val psiFile: PsiFile) : KtSourceFile {
    override val name: String
        get() = psiFile.name

    override val path: String?
        get() = psiFile.virtualFile?.path

    override fun getContentsAsStream(): InputStream = psiFile.virtualFile.inputStream
}

class KtVirtualFileSourceFile(val virtualFile: VirtualFile) : KtSourceFile {
    override val name: String
        get() = virtualFile.name

    override val path: String
        get() = virtualFile.path

    override fun getContentsAsStream(): InputStream = virtualFile.inputStream
}

class KtIoFileSourceFile(val file: File) : KtSourceFile {
    override val name: String
        get() = file.name
    override val path: String
        get() = FileUtilRt.toSystemIndependentName(file.path)

    override fun getContentsAsStream(): InputStream = file.inputStream()
}

class KtInMemoryTextSourceFile(
    override val name: String,
    override val path: String?,
    val text: CharSequence
) : KtSourceFile {
    override fun getContentsAsStream(): InputStream = ByteArrayInputStream(text.toString().toByteArray())
}
