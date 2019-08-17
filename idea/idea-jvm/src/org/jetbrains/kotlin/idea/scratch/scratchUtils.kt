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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.actions.KOTLIN_WORKSHEET_EXTENSION
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputHandler
import org.jetbrains.kotlin.idea.scratch.ui.ScratchTopPanel
import org.jetbrains.kotlin.psi.UserDataProperty

internal val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.scratch")
internal fun Logger.printDebugMessage(str: String) {
    if (isDebugEnabled) debug("SCRATCH: $str")
}

val VirtualFile.isKotlinWorksheet: Boolean
    get() = name.endsWith(".$KOTLIN_WORKSHEET_EXTENSION")

val VirtualFile.isKotlinScratch: Boolean
    get() = ScratchFileService.getInstance().getRootType(this) is ScratchRootType

fun getEditorWithoutScratchFile(fileManager: FileEditorManager, virtualFile: VirtualFile): TextEditor? {
    val editor = fileManager.getSelectedEditor(virtualFile) as? TextEditor
    if (editor?.getScratchFile() != null) return null
    return editor
}

@TestOnly
fun getScratchPanelFromEditorSelectedForFile(fileManager: FileEditorManager, virtualFile: VirtualFile): ScratchTopPanel? {
    val editor = fileManager.getSelectedEditor(virtualFile) as? TextEditor ?: return null
    return editor.scratchTopPanel
}

fun getScratchFileFromEditorSelectedForFile(fileManager: FileEditorManager, virtualFile: VirtualFile): ScratchFile? {
    val editor = fileManager.getSelectedEditor(virtualFile) as? TextEditor ?: return null
    return editor.getScratchFile()
}

fun getAllEditorsWithScratchFiles(project: Project): List<TextEditor> =
    FileEditorManager.getInstance(project).allEditors
        .filterIsInstance<TextEditor>()
        .filter { it.getScratchFile() != null }

fun getScratchFileFromSelectedEditor(project: Project): ScratchFile? {
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
    return TextEditorProvider.getInstance().getTextEditor(editor).getScratchFile()
}

fun TextEditor.getScratchFile(): ScratchFile? {
    return scratchTopPanel?.scratchFile
}

fun TextEditor.addScratchPanel(panel: ScratchTopPanel) {
    scratchTopPanel = panel
    FileEditorManager.getInstance(panel.scratchFile.project).addTopComponent(this, panel.component)

    Disposer.register(this, panel)
}

fun TextEditor.attachOutputHandler(handler: ScratchOutputHandler) {
    outputHandler = handler
}

val TextEditor.attachedOutputHandler: ScratchOutputHandler? get() = outputHandler

private var TextEditor.scratchTopPanel: ScratchTopPanel? by UserDataProperty<TextEditor, ScratchTopPanel>(Key.create("scratch.panel"))

private var TextEditor.outputHandler: ScratchOutputHandler? by UserDataProperty<TextEditor, ScratchOutputHandler>(Key.create("scratch.output.handler"))