/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.HyperlinkLabel
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.psi.UserDataProperty
import kotlin.script.experimental.dependencies.ScriptDependencies

fun VirtualFile.removeScriptDependenciesNotificationPanel(project: Project) {
    val editor = FileEditorManager.getInstance(project).getSelectedEditor(this) ?: return
    editor.notificationPanel?.let { FileEditorManager.getInstance(project).removeTopComponent(editor, it) }
    editor.notificationPanel = null
}

fun VirtualFile.addScriptDependenciesNotificationPanel(
    dependencies: ScriptDependencies,
    project: Project,
    onClick: (ScriptDependencies) -> Unit
) {
    val editor = FileEditorManager.getInstance(project).getSelectedEditor(this) ?: return
    val existingPanel = editor.notificationPanel
    if (existingPanel != null) {
        if (existingPanel.dependencies == dependencies) return
        editor.notificationPanel?.let {
            FileEditorManager.getInstance(project).removeTopComponent(editor, it)
        }
    }

    val panel = NewScriptDependenciesNotificationPanel(onClick, dependencies, project)
    editor.notificationPanel = panel
    FileEditorManager.getInstance(project).addTopComponent(editor, panel)
}

private var FileEditor.notificationPanel: NewScriptDependenciesNotificationPanel? by UserDataProperty<FileEditor, NewScriptDependenciesNotificationPanel>(Key.create("script.dependencies.panel"))

private class NewScriptDependenciesNotificationPanel(
    onClick: (ScriptDependencies) -> Unit,
    val dependencies: ScriptDependencies,
    project: Project
) : EditorNotificationPanel() {

    init {
        setText("There are new script dependencies available.")
        createComponentActionLabel("Apply dependencies") {
            onClick(dependencies)
        }

        createComponentActionLabel("Enable auto-reload") {
            onClick(dependencies)
            KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled = true
        }
    }

    private fun EditorNotificationPanel.createComponentActionLabel(labelText: String, callback: (HyperlinkLabel) -> Unit) {
        val label: Ref<HyperlinkLabel> = Ref.create()
        val action = Runnable {
            callback(label.get())
        }
        label.set(createActionLabel(labelText, action))
    }
}