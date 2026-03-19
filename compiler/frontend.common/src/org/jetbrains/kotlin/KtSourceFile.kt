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

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

class KtPsiSourceFile(val psiFile: PsiFile) : KtSourceFile {
    override val name: String
        get() = psiFile.name

    override val path: String?
        get() = psiFile.virtualFile?.path

    override fun getContentsAsStream(): InputStream = psiFile.virtualFile.inputStream

    override fun equals(other: Any?): Boolean {
        return this === other || (other as? KtPsiSourceFile)?.psiFile == psiFile
    }

    override fun hashCode(): Int {
        return psiFile.hashCode()
    }
}

class KtVirtualFileSourceFile(val virtualFile: VirtualFile) : KtSourceFile {
    override val name: String
        get() = virtualFile.name

    override val path: String
        get() = virtualFile.path

    override fun getContentsAsStream(): InputStream = virtualFile.inputStream

    override fun equals(other: Any?): Boolean {
        return this === other || (other as? KtVirtualFileSourceFile)?.virtualFile == virtualFile
    }

    override fun hashCode(): Int {
        return virtualFile.hashCode()
    }
}

class KtIoFileSourceFile(val file: File) : KtSourceFile {
    override val name: String
        get() = file.name
    override val path: String
        get() = FileUtilRt.toSystemIndependentName(file.path)

    override fun getContentsAsStream(): InputStream = file.inputStream()

    override fun equals(other: Any?): Boolean {
        return this === other || (other as? KtIoFileSourceFile)?.file == file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }
}

class KtInMemoryTextSourceFile(
    override val name: String,
    override val path: String?,
    val text: CharSequence
) : KtSourceFile {
    override fun getContentsAsStream(): InputStream = ByteArrayInputStream(text.toString().toByteArray())

    override fun equals(other: Any?): Boolean {
        return this === other || (other is KtInMemoryTextSourceFile && other.text == text && other.name == name && other.path == path)
    }

    override fun hashCode(): Int {
        return text.hashCode() + 17 * name.hashCode() + 31 * (path?.hashCode() ?: 0)
    }
}
