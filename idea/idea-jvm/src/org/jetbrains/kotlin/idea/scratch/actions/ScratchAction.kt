/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import org.jetbrains.kotlin.idea.scratch.getScratchPanel
import org.jetbrains.kotlin.idea.scratch.ui.ScratchTopPanel
import javax.swing.Icon

abstract class ScratchAction(message: String, icon: Icon) : AnAction(message, message, icon) {
    override fun update(e: AnActionEvent) {
        val presentation = e.presentation

        val project = e.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val scratchPanel = getScratchPanel(editor)

        presentation.isVisible = scratchPanel != null
    }

    protected fun getScratchPanel(editor: Editor): ScratchTopPanel? {
        return TextEditorProvider.getInstance().getTextEditor(editor).getScratchPanel()
    }
}