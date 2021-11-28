/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.source

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SourceFile

interface PsiSourceElement : SourceElement {
    val psi: PsiElement?

    override fun getContainingFile(): SourceFile = psi?.containingFile?.let(::PsiSourceFile) ?: SourceFile.NO_SOURCE_FILE
}

class PsiSourceFile(val psiFile: PsiFile) : SourceFile {
    override fun equals(other: Any?): Boolean = other is PsiSourceFile && psiFile == other.psiFile

    override fun hashCode(): Int = psiFile.hashCode()

    override fun toString(): String = psiFile.virtualFile.path

    override fun getName(): String? = psiFile.virtualFile?.name
}

fun SourceElement.getPsi(): PsiElement? = (this as? PsiSourceElement)?.psi
