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

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.ir.SourceLocationManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrFile
import java.util.*

class PsiSourceManager : SourceLocationManager {
    class PsiFileEntry(psiFile: PsiFile) : SourceLocationManager.FileEntry {
        private val psiFileName = psiFile.virtualFile?.path ?: psiFile.name

        override val maxOffset: Int
        private val lineStartOffsets: IntArray

        init {
            val document = psiFile.viewProvider.document ?: throw AssertionError("No document for $psiFile")

            maxOffset = document.textLength

            lineStartOffsets = (0 .. document.lineCount - 1)
                    .map { document.getLineStartOffset(it) }
                    .toIntArray()
        }

        override fun getLineNumber(offset: Int): Int {
            val index = lineStartOffsets.binarySearch(offset)
            return if (index >= 0) index else -index - 1
        }

        override fun getColumnNumber(offset: Int): Int {
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

        fun getRecognizableName(): String = psiFileName

        override fun toString(): String = getRecognizableName()
    }

    private val fileEntriesByPsiFile = HashMap<PsiFile, PsiFileEntry>()
    private val fileEntriesByIrFile = HashMap<IrFile, PsiFileEntry>()

    fun createFileEntry(psiFile: PsiFile): PsiFileEntry {
        if (psiFile in fileEntriesByPsiFile) error("PsiFileEntry is already created for $psiFile")
        val newEntry = PsiFileEntry(psiFile)
        fileEntriesByPsiFile[psiFile] = newEntry
        return newEntry
    }

    fun putFileEntry(irFile: IrFile, fileEntry: PsiFileEntry) {
        fileEntriesByIrFile[irFile] = fileEntry
    }

    fun getOrCreateFileEntry(psiFile: PsiFile): PsiFileEntry =
            fileEntriesByPsiFile.getOrElse(psiFile) { createFileEntry(psiFile) }

    fun getFileEntry(psiFile: PsiFile): PsiFileEntry? =
            fileEntriesByPsiFile[psiFile]

    override fun getFileEntry(irFile: IrFile): SourceLocationManager.FileEntry =
            fileEntriesByIrFile[irFile]!!
}
