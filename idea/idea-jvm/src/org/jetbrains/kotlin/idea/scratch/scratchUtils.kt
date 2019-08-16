/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.actions.KOTLIN_WORKSHEET_EXTENSION
import org.jetbrains.kotlin.idea.scratch.ui.ScratchPanelListener
import org.jetbrains.kotlin.idea.scratch.ui.ScratchTopPanel
import org.jetbrains.kotlin.idea.syncPublisherWithDisposeCheck
import org.jetbrains.kotlin.psi.UserDataProperty

internal val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.scratch")
internal fun Logger.printDebugMessage(str: String) {
    if (isDebugEnabled) debug("SCRATCH: $str")
}

val VirtualFile.isKotlinWorksheet: Boolean
    get() = name.endsWith(".$KOTLIN_WORKSHEET_EXTENSION")

val VirtualFile.isKotlinScratch: Boolean
    get() = ScratchFileService.getInstance().getRootType(this) is ScratchRootType

fun getEditorWithoutScratchPanel(fileManager: FileEditorManager, virtualFile: VirtualFile): TextEditor? {
    val editor = fileManager.getSelectedEditor(virtualFile) as? TextEditor
    if (editor?.scratchTopPanel != null) return null
    return editor
}

fun getEditorWithScratchPanel(fileManager: FileEditorManager, virtualFile: VirtualFile): Pair<TextEditor, ScratchTopPanel>? {
    val editor = fileManager.getSelectedEditor(virtualFile) as? TextEditor ?: return null
    val scratchTopPanel = editor.scratchTopPanel ?: return null
    return editor to scratchTopPanel
}

fun getAllEditorsWithScratchPanel(project: Project): List<Pair<TextEditor, ScratchTopPanel>> =
    FileEditorManager.getInstance(project).allEditors.filterIsInstance<TextEditor>().mapNotNull {
        val panel = it.scratchTopPanel
        if (panel != null) it to panel else null
    }

fun getScratchPanelFromSelectedEditor(project: Project): ScratchTopPanel? {
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
    return TextEditorProvider.getInstance().getTextEditor(editor).getScratchPanel()
}

fun TextEditor.getScratchPanel(): ScratchTopPanel? {
    return scratchTopPanel
}

fun TextEditor.addScratchPanel(panel: ScratchTopPanel) {
    scratchTopPanel = panel
    FileEditorManager.getInstance(panel.scratchFile.project).addTopComponent(this, panel.component)

    Disposer.register(this, panel)
    panel.scratchFile.project.syncPublisherWithDisposeCheck(ScratchPanelListener.TOPIC).panelAdded(panel)
}

fun TextEditor.removeScratchPanel() {
    scratchTopPanel?.let { FileEditorManager.getInstance(it.scratchFile.project).removeTopComponent(this, it.component) }
    scratchTopPanel = null
}

private var TextEditor.scratchTopPanel: ScratchTopPanel? by UserDataProperty<TextEditor, ScratchTopPanel>(Key.create("scratch.panel"))
