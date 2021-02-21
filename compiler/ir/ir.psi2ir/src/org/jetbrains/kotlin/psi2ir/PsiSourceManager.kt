/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.fileOrNull
import kotlin.reflect.KClass

object PsiSourceManager {
    class PsiFileEntry(val psiFile: PsiFile) : IrFileEntry {
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
            if (offset < 0) return -1
            val index = lineStartOffsets.binarySearch(offset)
            return if (index >= 0) index else -index - 2
        }

        override fun getColumnNumber(offset: Int): Int {
            if (offset < 0) return -1
            val lineNumber = getLineNumber(offset)
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

    fun <E : PsiElement> findPsiElement(irElement: IrElement, irFile: IrFile, psiElementClass: KClass<E>): E? {
        val psiFileEntry = irFile.fileEntry as? PsiFileEntry ?: return null
        return psiFileEntry.findPsiElement(irElement, psiElementClass)
    }

    fun findPsiElement(irElement: IrElement, irFile: IrFile): PsiElement? {
        val psiFileEntry = irFile.fileEntry as? PsiFileEntry ?: return null
        return psiFileEntry.findPsiElement(irElement)
    }

    fun <E : PsiElement> findPsiElement(irElement: IrElement, irDeclaration: IrDeclaration, psiElementClass: KClass<E>): E? {
        val irFile = irDeclaration.fileOrNull ?: return null
        return findPsiElement(irElement, irFile, psiElementClass)
    }

    fun findPsiElement(irElement: IrElement, irDeclaration: IrDeclaration): PsiElement? {
        val irFile = irDeclaration.fileOrNull ?: return null
        return findPsiElement(irElement, irFile)
    }

    fun <E : PsiElement> findPsiElement(irDeclaration: IrDeclaration, psiElementClass: KClass<E>): E? =
        findPsiElement(irDeclaration, irDeclaration, psiElementClass)

    fun findPsiElement(irDeclaration: IrDeclaration): PsiElement? =
        findPsiElement(irDeclaration, irDeclaration)
}
