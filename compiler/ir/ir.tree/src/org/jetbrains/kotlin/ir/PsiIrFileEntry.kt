/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import kotlin.reflect.KClass

class PsiIrFileEntry(val psiFile: PsiFile) : IrFileEntry {
    private val psiFileName = psiFile.virtualFile?.path ?: psiFile.name

    override val maxOffset: Int
    private val lineStartOffsets: IntArray
    private val fileViewProvider = psiFile.viewProvider

    init {
        val document = fileViewProvider.document ?: throw AssertionError("No document for $psiFile")
        maxOffset = document.textLength
        lineStartOffsets = IntArray(document.lineCount) { document.getLineStartOffset(it) }
    }

    override fun getLineNumber(offset: Int): Int {
        if (offset < 0) return UNDEFINED_LINE_NUMBER
        val index = lineStartOffsets.binarySearch(offset)
        return if (index >= 0) index else -index - 2
    }

    override fun getColumnNumber(offset: Int): Int {
        if (offset < 0) return UNDEFINED_COLUMN_NUMBER
        val lineNumber = getLineNumber(offset)
        if (lineNumber < 0) return UNDEFINED_COLUMN_NUMBER
        return offset - lineStartOffsets[lineNumber]
    }

    override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int): SourceRangeInfo =
        SourceRangeInfo(
            filePath = getRecognizableName(),
            startOffset = beginOffset,
            startLineNumber = getLineNumber(beginOffset),
            startColumnNumber = getColumnNumber(beginOffset),
            endOffset = endOffset,
            endLineNumber = getLineNumber(endOffset),
            endColumnNumber = getColumnNumber(endOffset)
        )

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
