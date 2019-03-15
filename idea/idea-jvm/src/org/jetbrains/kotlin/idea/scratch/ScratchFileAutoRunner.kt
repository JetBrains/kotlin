/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchAction
import org.jetbrains.kotlin.idea.scratch.actions.ScratchCompilationSupport
import org.jetbrains.kotlin.idea.scratch.ui.ScratchTopPanel

class ScratchFileAutoRunner(private val project: Project) : DocumentListener {
    companion object {
        fun addListener(project: Project, editor: TextEditor) {
            if (editor.getScratchPanel() != null) {
                editor.editor.document.addDocumentListener(getInstance(project))
                Disposer.register(editor, Disposable {
                    editor.editor.document.removeDocumentListener(getInstance(project))
                })
            }
        }

        private fun getInstance(project: Project) = ServiceManager.getService(project, ScratchFileAutoRunner::class.java)

        private const val auto_run_delay = 2000
    }

    private val myAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)

    override fun documentChanged(event: DocumentEvent) {
        val file = FileDocumentManager.getInstance().getFile(event.document) ?: return

        if (project.isDisposed) return
        val panel = getScratchPanel(file, project) ?: return
        if (!panel.scratchFile.options.isInteractiveMode) return

        if (isScratchChanged(project, file)) {
            runScratch(panel)
        }
    }

    private fun runScratch(panel: ScratchTopPanel) {
        myAlarm.cancelAllRequests()

        if (ScratchCompilationSupport.isInProgress(panel.scratchFile)) {
            ScratchCompilationSupport.forceStop()
        }

        myAlarm.addRequest(
            {
                val psiFile = panel.scratchFile.getPsiFile()
                if (psiFile != null && psiFile.isValid && !panel.scratchFile.hasErrors()) {
                    RunScratchAction.doAction(panel, true)
                }
            }, auto_run_delay, true
        )
    }

    private fun getScratchPanel(file: VirtualFile, project: Project): ScratchTopPanel? {
        return getEditorWithScratchPanel(FileEditorManager.getInstance(project), file)?.second
    }
}