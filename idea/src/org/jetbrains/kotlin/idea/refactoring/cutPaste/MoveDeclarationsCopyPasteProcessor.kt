/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.cutPaste

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import java.awt.datatransfer.Transferable

class MoveDeclarationsCopyPasteProcessor : CopyPastePostProcessor<MoveDeclarationsTransferableData>() {
    companion object {
        private val LOG = Logger.getInstance(MoveDeclarationsCopyPasteProcessor::class.java)

        fun rangeToDeclarations(file: KtFile, startOffset: Int, endOffset: Int): List<KtNamedDeclaration> {
            val elementsInRange = file.elementsInRange(TextRange(startOffset, endOffset))
            val meaningfulElements = elementsInRange.filterNot { it is PsiWhiteSpace || it is PsiComment }
            if (meaningfulElements.isEmpty()) return emptyList()
            if (!meaningfulElements.all { it is KtNamedDeclaration }) return emptyList()
            @Suppress("UNCHECKED_CAST")
            return meaningfulElements as List<KtNamedDeclaration>
        }
    }

    override fun collectTransferableData(
            file: PsiFile,
            editor: Editor,
            startOffsets: IntArray,
            endOffsets: IntArray
    ): List<MoveDeclarationsTransferableData> {
        if (file !is KtFile) return emptyList()
        if (startOffsets.size != 1) return emptyList()

        val declarations = rangeToDeclarations(file, startOffsets[0], endOffsets[0])
        if (declarations.isEmpty()) return emptyList()

        val parent = declarations.map { it.parent }.distinct().singleOrNull() ?: return emptyList()
        val sourceObjectFqName = when (parent) {
            is KtFile -> null
            is KtClassBody -> (parent.parent as? KtObjectDeclaration)?.fqName?.asString() ?: return emptyList()
            else -> return emptyList()
        }

        if (declarations.any { it.name == null }) return emptyList()
        val declarationNames = declarations.map { it.name!! }.toSet()

        val stubTexts = declarations.map { MoveDeclarationsTransferableData.STUB_RENDERER.render(it.unsafeResolveToDescriptor()) }
        return listOf(MoveDeclarationsTransferableData(file.virtualFile.url, sourceObjectFqName, stubTexts, declarationNames))
    }

    override fun extractTransferableData(content: Transferable): List<MoveDeclarationsTransferableData> {
        try {
            if (content.isDataFlavorSupported(MoveDeclarationsTransferableData.DATA_FLAVOR)) {
                return listOf(content.getTransferData(MoveDeclarationsTransferableData.DATA_FLAVOR) as MoveDeclarationsTransferableData)
            }
        }
        catch (e: Throwable) {
            LOG.error(e)
        }
        return emptyList()
    }

    override fun processTransferableData(
            project: Project,
            editor: Editor,
            bounds: RangeMarker,
            caretOffset: Int,
            indented: Ref<Boolean>,
            values: List<MoveDeclarationsTransferableData>
    ) {
        val data = values.single()

        fun putCookie() {
            if (bounds.isValid) {
                val cookie = MoveDeclarationsEditorCookie(data, bounds, PsiModificationTracker.SERVICE.getInstance(project).modificationCount)
                editor.putUserData(MoveDeclarationsEditorCookie.KEY, cookie)
            }
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            putCookie()
        }
        else {
            // in real application we put cookie later to allow all other paste handlers do their work (because modificationCount will change)
            ApplicationManager.getApplication().invokeLater(::putCookie)
        }
    }
}

