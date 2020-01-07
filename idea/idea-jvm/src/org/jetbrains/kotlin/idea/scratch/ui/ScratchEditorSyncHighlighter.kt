/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.scratch.output.highlightLines

interface ScratchEditorLinesTranslator {
    fun previewLineToSourceLines(previewLine: Int): Pair<Int, Int>?
    fun sourceLineToPreviewLines(sourceLine: Int): Pair<Int, Int>?
}

fun configureSyncHighlighting(sourceEditor: EditorEx, previewEditor: EditorEx, translator: ScratchEditorLinesTranslator) {
    configureExclusiveCaretRowHighlighting(sourceEditor, previewEditor)
    configureSourceAndPreviewHighlighting(sourceEditor, previewEditor, translator)
}

private fun configureSourceAndPreviewHighlighting(
    sourceEditor: EditorEx,
    previewEditor: EditorEx,
    translator: ScratchEditorLinesTranslator
) {
    val syncHighlighter = ScratchEditorSyncHighlighter.create(sourceEditor, previewEditor, translator)

    configureHighlightUpdateOnDocumentChange(sourceEditor, previewEditor, syncHighlighter)
    configureSourceToPreviewHighlighting(sourceEditor, syncHighlighter)
    configurePreviewToSourceHighlighting(previewEditor, syncHighlighter)
}

/**
 * Configures editors such that only one of them have caret row highlighting enabled.
 */
private fun configureExclusiveCaretRowHighlighting(sourceEditor: EditorEx, previewEditor: EditorEx) {
    val exclusiveCaretHighlightingListener = object : FocusChangeListener {
        override fun focusLost(editor: Editor) {}

        override fun focusGained(editor: Editor) {
            sourceEditor.settings.isCaretRowShown = false
            previewEditor.settings.isCaretRowShown = false

            editor.settings.isCaretRowShown = true
        }
    }

    sourceEditor.addFocusListener(exclusiveCaretHighlightingListener)
    previewEditor.addFocusListener(exclusiveCaretHighlightingListener)
}

/**
 * When source or preview documents change, we need to update highlighting, because
 * expression output may become bigger.
 *
 * We can do that only when document is fully committed, so [ScratchFile.getExpressions] will return correct expressions
 * with correct PSIs.
 */
private fun configureHighlightUpdateOnDocumentChange(
    sourceEditor: EditorEx,
    previewEditor: EditorEx,
    highlighter: ScratchEditorSyncHighlighter
) {
    val project = sourceEditor.project!!
    val updateHighlightOnDocumentChangeListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            PsiDocumentManager.getInstance(project).performWhenAllCommitted {
                highlighter.highlightByCurrentlyFocusedEditor()
            }
        }
    }

    previewEditor.document.addDocumentListener(updateHighlightOnDocumentChangeListener, project)
    sourceEditor.document.addDocumentListener(updateHighlightOnDocumentChangeListener, project)
}

/**
 * When caret in [sourceEditor] is moved, highlight is recalculated.
 *
 * When focus is switched to the [sourceEditor], highlight is recalculated,
 * because it is possible to switch focus without changing cursor position,
 * which would lead to the outdated highlighting.
 */
private fun configureSourceToPreviewHighlighting(sourceEditor: EditorEx, highlighter: ScratchEditorSyncHighlighter) {
    sourceEditor.caretModel.addCaretListener(object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            highlighter.highlightPreviewBySource()
        }
    })

    sourceEditor.addFocusListener(object : FocusChangeListener {
        override fun focusLost(editor: Editor) {}

        override fun focusGained(editor: Editor) {
            highlighter.highlightPreviewBySource()
        }
    })
}

/**
 * When caret in [previewEditor] is moved, highlight is recalculated.
 *
 * When focus is switched to the [previewEditor], highlight is recalculated,
 * because it is possible to switch focus without changing cursor position,
 * which would lead to the outdated highlighting.
 */
private fun configurePreviewToSourceHighlighting(previewEditor: EditorEx, highlighter: ScratchEditorSyncHighlighter) {
    previewEditor.caretModel.addCaretListener(object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            highlighter.highlightSourceByPreview()
        }
    })

    previewEditor.addFocusListener(object : FocusChangeListener {
        override fun focusLost(editor: Editor) {}

        override fun focusGained(editor: Editor) {
            highlighter.highlightSourceByPreview()
        }
    })
}

private class ScratchEditorsState(private val sourceEditor: EditorEx, private val previewEditor: EditorEx) : FocusChangeListener {
    private var lastFocusedEditor: Editor = sourceEditor

    enum class FocusedEditor {
        SOURCE, PREVIEW
    }

    init {
        sourceEditor.addFocusListener(this)
        previewEditor.addFocusListener(this)
    }

    val sourceEditorCaretLine: Int? get() = sourceEditor.caretModel.allCarets.singleOrNull()?.logicalPosition?.line
    val previewEditorCaretLine: Int? get() = previewEditor.caretModel.allCarets.singleOrNull()?.logicalPosition?.line
    val focusedEditor: FocusedEditor get() = if (lastFocusedEditor === sourceEditor) FocusedEditor.SOURCE else FocusedEditor.PREVIEW

    override fun focusLost(editor: Editor) {}

    override fun focusGained(editor: Editor) {
        lastFocusedEditor = editor
    }
}

private class ScratchEditorSyncHighlighter private constructor(
    private val state: ScratchEditorsState,
    private val sourceHighlighter: EditorLinesHighlighter,
    private val previewHighlighter: EditorLinesHighlighter,
    private val translator: ScratchEditorLinesTranslator
) {
    fun highlightSourceByPreview() {
        clearAllHighlights()

        state.previewEditorCaretLine?.let(::highlightSourceByPreviewLine)
    }

    fun highlightPreviewBySource() {
        clearAllHighlights()

        state.sourceEditorCaretLine?.let(::highlightPreviewBySourceLine)
    }

    fun highlightByCurrentlyFocusedEditor() {
        when (state.focusedEditor) {
            ScratchEditorsState.FocusedEditor.SOURCE -> highlightPreviewBySource()
            ScratchEditorsState.FocusedEditor.PREVIEW -> highlightSourceByPreview()
        }
    }

    private fun highlightSourceByPreviewLine(selectedPreviewLine: Int) {
        val (from, to) = translator.sourceLineToPreviewLines(selectedPreviewLine) ?: return

        sourceHighlighter.highlightLines(from, to)
    }

    private fun highlightPreviewBySourceLine(selectedSourceLine: Int) {
        val (from, to) = translator.previewLineToSourceLines(selectedSourceLine) ?: return

        previewHighlighter.highlightLines(from, to)
    }

    private fun clearAllHighlights() {
        sourceHighlighter.clearHighlights()
        previewHighlighter.clearHighlights()
    }

    companion object {
        fun create(
            sourceEditor: EditorEx,
            previewEditor: EditorEx,
            translator: ScratchEditorLinesTranslator
        ): ScratchEditorSyncHighlighter {
            return ScratchEditorSyncHighlighter(
                state = ScratchEditorsState(sourceEditor, previewEditor),
                sourceHighlighter = EditorLinesHighlighter(sourceEditor),
                previewHighlighter = EditorLinesHighlighter(previewEditor),
                translator = translator
            )
        }
    }
}

private class EditorLinesHighlighter(private val targetEditor: Editor) {
    private var activeHighlight: RangeHighlighter? = null

    fun clearHighlights() {
        activeHighlight?.let(targetEditor.markupModel::removeHighlighter)
        activeHighlight = null
    }

    fun highlightLines(lineStart: Int, lineEnd: Int) {
        clearHighlights()

        val highlightColor = targetEditor.colorsScheme.getColor(EditorColors.CARET_ROW_COLOR) ?: return

        activeHighlight = targetEditor.markupModel.highlightLines(
            lineStart,
            lineEnd,
            TextAttributes().apply { backgroundColor = highlightColor },
            HighlighterTargetArea.LINES_IN_RANGE
        )
    }
}
