/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import kotlin.reflect.KClass

class PsiIrFileEntry(val psiFile: PsiFile) : AbstractIrFileEntry() {
    private val psiFileName = psiFile.virtualFile?.path ?: psiFile.name

    override val maxOffset: Int
    override val lineStartOffsets: IntArray
    private val fileViewProvider = psiFile.viewProvider

    init {
        val document = fileViewProvider.document ?: throw AssertionError("No document for $psiFile")
        maxOffset = document.textLength
        lineStartOffsets = IntArray(document.lineCount) { document.getLineStartOffset(it) }
    }

    private fun getRecognizableName(): String = psiFileName

    override val name: String get() = getRecognizableName()

    override fun toString(): String = getRecognizableName()

    fun getLineOffsets() = lineStartOffsets.copyOf()

    fun findPsiElement(irElement: IrElement): PsiElement? {
        var psiElement = fileViewProvider.findElementAt(irElement.startOffset)
        while (psiElement != null) {
            if (irElement.endOffset == psiElement.textRange?.endOffset) break
            psiElement = psiElement.parent
        }
        return psiElement
    }

    fun <E : PsiElement> findPsiElement(irElement: IrElement, psiElementClass: KClass<E>): E? =
        findPsiElement(irElement)?.let {
            PsiTreeUtil.getParentOfType(it, psiElementClass.java, false)
        }
}
