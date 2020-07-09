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
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener.Companion.LISTENER
import org.jetbrains.kotlin.idea.core.script.isScriptChangesNotifierDisabled

internal class ScriptChangesNotifier(
    private val project: Project
) {
    private val scriptsQueue = Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)
    private val scriptChangesListenerDelay = 1400

    init {
        listenForChangesInScripts()
    }

    private fun listenForChangesInScripts() {
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    runScriptDependenciesUpdateIfNeeded(file)
                }

                override fun selectionChanged(event: FileEditorManagerEvent) {
                    event.newFile?.let { runScriptDependenciesUpdateIfNeeded(it) }
                }

                private fun runScriptDependenciesUpdateIfNeeded(file: VirtualFile) {
                    if (ApplicationManager.getApplication().isUnitTestMode) {
                        getListener(project, file)?.editorActivated(file)
                    } else {
                        AppExecutorUtil.getAppExecutorService().submit {
                            getListener(project, file)?.editorActivated(file)
                        }
                    }
                }
            },
        )

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val document = event.document
                    val file = FileDocumentManager.getInstance().getFile(document)?.takeIf { it.isInLocalFileSystem } ?: return

                    // Do not listen for changes in files that are not open
                    if (file !in FileEditorManager.getInstance(project).openFiles) {
                        return
                    }

                    if (ApplicationManager.getApplication().isUnitTestMode) {
                        getListener(project, file)?.documentChanged(file)
                    } else {
                        scriptsQueue.cancelAllRequests()
                        scriptsQueue.addRequest(
                            { getListener(project, file)?.documentChanged(file) },
                            scriptChangesListenerDelay,
                            true,
                        )
                    }
                }
            },
            project.messageBus.connect(),
        )
    }

    private val defaultListener = DefaultScriptChangeListener(project)
    private val listeners: Collection<ScriptChangeListener>
        get() = mutableListOf<ScriptChangeListener>().apply {
            addAll(LISTENER.getPoint(project).extensionList)
            add(defaultListener)
        }

    private fun getListener(project: Project, file: VirtualFile): ScriptChangeListener? {
        if (project.isDisposed || areListenersDisabled()) return null

        return listeners.firstOrNull { it.isApplicable(file) }
    }

    private fun areListenersDisabled(): Boolean {
        return ApplicationManager.getApplication().isUnitTestMode && ApplicationManager.getApplication()
            .isScriptChangesNotifierDisabled == true
    }
}
