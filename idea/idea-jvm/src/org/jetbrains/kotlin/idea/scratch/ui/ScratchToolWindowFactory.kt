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

package org.jetbrains.kotlin.idea.scratch.ui

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager

class ScratchToolWindowFactory : ToolWindowFactory {
    companion object {
        val ID = "Scratch error output"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val consoleView = ConsoleViewImpl(project, true)
        toolWindow.isToHideOnEmptyContent = true

        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(consoleView.component, null, false)
        contentManager.addContent(content)
        val editor = consoleView.editor
        if (editor is EditorEx) {
            editor.isRendererMode = true
        }

        Disposer.register(project, consoleView)
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, getFileEditorManagerListener(toolWindow))
    }

    private fun getFileEditorManagerListener(toolWindow: ToolWindow): FileEditorManagerListener {
        return object : FileEditorManagerListener {
            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                toolWindow.setAvailable(false, {})
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                toolWindow.setAvailable(false, {})
            }
        }
    }
}

fun showToolWindow(
        project: Project,
        message: String,
        type: ConsoleViewContentType
) {
    if (ApplicationManager.getApplication().isUnitTestMode) return

    ApplicationManager.getApplication().invokeLater {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        var window: ToolWindow? = toolWindowManager.getToolWindow(ScratchToolWindowFactory.ID)
        if (window == null) {
            toolWindowManager.registerToolWindow(ScratchToolWindowFactory.ID, true, ToolWindowAnchor.BOTTOM)
            window = toolWindowManager.getToolWindow(ScratchToolWindowFactory.ID)
            ScratchToolWindowFactory().createToolWindowContent(project, window!!)
        }

        val contents = window.contentManager.contents
        for (content in contents) {
            val component = content.component
            if (component is ConsoleViewImpl) {
                component.clear()
                component.print(message, type)
                window.setAvailable(true, null)
                window.show(null)
            }
        }
    }
}