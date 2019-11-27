/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo.custom

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.core.util.end
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import java.util.concurrent.ConcurrentHashMap

class KotlinCodeHintsModel(val project: Project) : EditorFactoryListener {
    companion object {
        fun getInstance(project: Project): KotlinCodeHintsModel =
            project.getComponent(KotlinCodeHintsModel::class.java) ?: error("Component `KotlinCodeHintsModel` is expected to be registered")
    }

    private class DocumentExtensionInfoModel(val document: Document) {
        private val lineEndMarkers = ConcurrentHashMap<RangeMarker, String>()

        fun markEndOfLine(lineEndOffset: Int, hint: String) {
            val endLineMarker = document.createRangeMarker(lineEndOffset, lineEndOffset)
            endLineMarker.isGreedyToRight = true

            lineEndMarkers[endLineMarker] = hint
        }

        fun getExtensionAtOffset(offset: Int): String? {
            return runReadAction {
                // Protect operations working with the document offsets

                lineEndMarkers
                    .entries
                    .firstOrNull { (marker, _) ->
                        val textRange = marker.range
                        if (textRange == null || !(textRange.startOffset <= offset && offset <= textRange.endOffset)) {
                            return@firstOrNull false
                        }
                        if (textRange.end > document.textLength) {
                            return@firstOrNull false
                        }

                        val document = marker.document
                        val hasNewLine = document.getText(textRange).contains('\n')
                        if (!hasNewLine) {
                            textRange.endOffset == offset
                        } else {
                            // New line may appear after session of fast typing with one or several enter hitting.
                            // We can't believe startOffset too because typing session may had started with
                            // typing adding several chars at the original line.
                            val originalLineNumber = document.getLineNumber(textRange.startOffset)
                            val currentOriginalLineEnd = document.getLineEndOffset(originalLineNumber)

                            currentOriginalLineEnd == offset
                        }
                    }
                    ?.value
            }
        }

        fun dispose() {
            val keys = lineEndMarkers.keys()
            ApplicationManager.getApplication().invokeLater {
                for (marker in keys) {
                    marker.dispose()
                }
            }
        }
    }

    private val documentModels =
        ContainerUtil.createConcurrentSoftMap<Document, DocumentExtensionInfoModel>()

    init {
        val editorFactory = EditorFactory.getInstance()
        editorFactory.addEditorFactoryListener(this, project)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        // Pass for other editor with the same document if present should re-add needed hints
        removeAll(event.editor.document)
    }

    fun getExtensionInfoAtOffset(editor: Editor): String? {
        return getExtensionInfo(editor.document, editor.caretModel.offset)
    }

    fun getExtensionInfo(document: Document, offset: Int): String? {
        return documentModels[document]?.getExtensionAtOffset(offset)
    }

    fun removeAll(document: Document) {
        documentModels.remove(document)?.dispose()
    }

    fun update(document: Document, actualHints: Map<PsiElement, String>) {
        if (actualHints.isEmpty()) {
            removeAll(document)
            return
        }

        val updatedModel = runReadAction {
            val model = DocumentExtensionInfoModel(document)
            for ((element, hint) in actualHints) {
                val lineNumber = document.getLineNumber(element.endOffset)
                val lineEndOffset = document.getLineEndOffset(lineNumber)

                model.markEndOfLine(lineEndOffset, hint)
            }

            model
        }

        documentModels.put(document, updatedModel)?.dispose()
    }
}