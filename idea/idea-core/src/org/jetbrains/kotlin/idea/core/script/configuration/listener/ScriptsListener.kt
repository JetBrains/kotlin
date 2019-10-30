/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.Alarm
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptConfigurationManagerImpl
import org.jetbrains.kotlin.idea.core.script.isScriptDependenciesUpdaterDisabled
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.isNonScript

class ScriptsListener(
    private val project: Project,
    private val scriptsManager: ScriptConfigurationManagerImpl
) {
    private val scriptsQueue = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)
    private val scriptChangesListenerDelay = 1400

    init {
        listenForChangesInScripts()
    }

    private fun listenForChangesInScripts() {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                runScriptDependenciesUpdateIfNeeded(file)
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                event.newFile?.let { runScriptDependenciesUpdateIfNeeded(it) }
            }

            private fun runScriptDependenciesUpdateIfNeeded(file: VirtualFile) {
                val ktFile = getKtFileToStartConfigurationUpdate(file) ?: return

                scriptsManager.updateConfigurationsIfNotCached(listOf(ktFile))
            }
        })

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val document = event.document
                val file = FileDocumentManager.getInstance().getFile(document)?.takeIf { it.isInLocalFileSystem } ?: return
                val ktFile = getKtFileToStartConfigurationUpdate(file) ?: return

                scriptsQueue.cancelAllRequests()

                scriptsQueue.addRequest(
                    { scriptsManager.updateConfigurationsIfNotCached(listOf(ktFile)) },
                    scriptChangesListenerDelay,
                    true
                )
            }
        }, project.messageBus.connect())
    }

    private fun getKtFileToStartConfigurationUpdate(file: VirtualFile): KtFile? {
        if (project.isDisposed || !file.isValid || file.isNonScript()) {
            return null
        }

        if (
            ApplicationManager.getApplication().isUnitTestMode &&
            ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled == true
        ) {
            return null
        }

        val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return null
        if (ProjectRootsUtil.isInProjectSource(ktFile, includeScriptsOutsideSourceRoots = true)) {
            return ktFile
        }

        return null
    }
}