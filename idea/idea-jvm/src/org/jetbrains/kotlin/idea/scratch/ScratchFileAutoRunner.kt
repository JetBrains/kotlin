/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.scratch.actions.RunScratchFromHereAction
import org.jetbrains.kotlin.idea.scratch.actions.ScratchCompilationSupport
import org.jetbrains.kotlin.idea.scratch.ui.findScratchFileEditorWithPreview

class ScratchFileAutoRunner(private val project: Project) : DocumentListener {
    companion object {
        fun addListener(project: Project, editor: TextEditor) {
            if (editor.getScratchFile() != null) {
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
        val scratchFile = getScratchFile(file, project) ?: return
        if (!scratchFile.options.isInteractiveMode) return

        if (!event.newFragment.isBlank()) {
            runScratch(scratchFile)
        }
    }

    private fun runScratch(scratchFile: ScratchFile) {
        myAlarm.cancelAllRequests()

        if (ScratchCompilationSupport.isInProgress(scratchFile) && !scratchFile.options.isRepl) {
            ScratchCompilationSupport.forceStop()
        }

        myAlarm.addRequest(
            {
                val psiFile = scratchFile.getPsiFile()
                if (psiFile != null && psiFile.isValid && !scratchFile.hasErrors()) {
                    if (scratchFile.options.isRepl) {
                        RunScratchFromHereAction.doAction(scratchFile)
                    } else {
                        RunScratchAction.doAction(scratchFile, true)
                    }
                }
            }, auto_run_delay, true
        )
    }

    private fun getScratchFile(file: VirtualFile, project: Project): ScratchFile? {
        val editor = FileEditorManager.getInstance(project).getSelectedEditor(file) as? TextEditor
        return editor?.findScratchFileEditorWithPreview()?.scratchFile
    }
}