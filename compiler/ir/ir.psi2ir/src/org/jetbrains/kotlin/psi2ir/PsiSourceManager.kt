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
import org.jetbrains.kotlin.ir.SourceLocation
import org.jetbrains.kotlin.ir.SourceLocationManager
import org.jetbrains.kotlin.ir.fileIndex
import org.jetbrains.kotlin.ir.fileOffset
import java.util.*

class PsiSourceManager : SourceLocationManager {
    class PsiFileEntry (val index: Int, val file: PsiFile) : SourceLocationManager.FileEntry {
        private val document = file.viewProvider.document

        override fun getLineNumber(sourceLocation: SourceLocation): Int =
                document?.getLineNumber(sourceLocation.fileOffset) ?: -1

        override fun getColumnNumber(sourceLocation: SourceLocation): Int =
                document?.getLineStartOffset(sourceLocation.fileOffset) ?: -1

        fun getRootSourceLocation(): SourceLocation =
                SourceLocation(index, 0)

        fun getSourceLocationForElement(element: PsiElement): SourceLocation =
                SourceLocation(index, element.textOffset)

        fun getRecognizableName(): String = file.virtualFile?.path ?: file.name

        override fun toString(): String = "#$index: ${getRecognizableName()}"
    }

    private val fileEntries = ArrayList<PsiFileEntry>()
    private val fileEntriesByPsiFile = HashMap<PsiFile, PsiFileEntry>()

    override fun getFileEntry(sourceLocation: Long): PsiFileEntry? =
            fileEntries.getOrNull(sourceLocation.fileIndex)

    fun createFileEntry(psiFile: PsiFile): PsiFileEntry {
        if (psiFile in fileEntriesByPsiFile) error("PsiFileEntry is already created for $psiFile")

        val newEntry = PsiFileEntry(fileEntries.size, psiFile)
        fileEntries.add(newEntry)
        fileEntriesByPsiFile[psiFile] = newEntry
        return newEntry
    }

    fun getOrCreateFileEntry(psiFile: PsiFile): PsiFileEntry =
            fileEntriesByPsiFile.getOrElse(psiFile) { createFileEntry(psiFile) }

    fun getFileEntry(psiFile: PsiFile): PsiFileEntry? = fileEntriesByPsiFile[psiFile]
}
