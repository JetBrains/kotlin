/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.ui.table.TableView
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.script.configuration.ScriptingSupportSpecificSettingsProvider
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsLocator.NotificationKind
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.kotlin.idea.scripting.gradle.settings.KotlinStandaloneScriptsModel.Companion.createModel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.plaf.BorderUIResource.TitledBorderUIResource

class GradleSettingsProvider(private val project: Project) : ScriptingSupportSpecificSettingsProvider() {
    override val title: String = KotlinIdeaGradleBundle.message("gradle.scripts.settings.title")

    override fun createConfigurable(): UnnamedConfigurable {
        return StandaloneScriptsUIComponent(project)
    }
}

class StandaloneScriptsUIComponent(private val project: Project) : UnnamedConfigurable {
    private val fileChooser: FileChooserDescriptor = object : FileChooserDescriptor(
        true, false, false,
        false, false, true
    ) {
        override fun isFileSelectable(file: VirtualFile): Boolean {
            val rootsManager = GradleBuildRootsManager.getInstance(project)
            val scriptUnderRoot = rootsManager.findScriptBuildRoot(file)
            val notificationKind = scriptUnderRoot?.notificationKind
            return notificationKind == NotificationKind.legacyOutside || notificationKind == NotificationKind.notEvaluatedInLastImport
        }
    }

    private var panel: JPanel? = null

    private val scriptsFromStorage = StandaloneScriptsStorage.getInstance(project)?.files?.toList() ?: emptyList()
    private val scriptsInTable = scriptsFromStorage.toMutableList()

    private val table: JBTable
    private val model: KotlinStandaloneScriptsModel

    init {
        model = createModel(scriptsInTable)

        table = TableView(model)
        table.preferredScrollableViewportSize = JBUI.size(300, -1)
        table.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        model.addTableModelListener(table)
    }

    override fun reset() {
        scriptsInTable.clear()
        scriptsInTable.addAll(scriptsFromStorage)
        model.items = scriptsInTable
    }

    override fun apply() {
        GradleBuildRootsManager.getInstance(project).updateStandaloneScripts {
            val previousScripts = scriptsFromStorage

            scriptsInTable
                .filterNot { previousScripts.contains(it) }
                .forEach { addStandaloneScript(it) }

            previousScripts
                .filterNot { scriptsInTable.contains(it) }
                .forEach { removeStandaloneScript(it) }
        }
    }

    override fun createComponent(): JComponent? {
        if (panel == null) {
            val panel = JPanel(BorderLayout())
            panel.border = TitledBorderUIResource(KotlinIdeaGradleBundle.message("standalone.scripts.settings.title"))
            panel.add(
                ToolbarDecorator.createDecorator(table)
                    .setAddAction { addScripts() }
                    .setRemoveAction { removeScripts() }
                    .setRemoveActionUpdater { table.selectedRow >= 0 }
                    .disableUpDownActions()
                    .createPanel()
            )
            this.panel = panel
        }
        return panel
    }

    override fun isModified(): Boolean {
        return scriptsFromStorage != scriptsInTable
    }

    override fun disposeUIResources() {
        panel = null
    }

    override fun cancel() {
        reset()
    }

    private fun addScripts() {
        val chosen = FileChooser.chooseFiles(fileChooser, project, null)
        for (chosenFile in chosen) {
            val path = chosenFile.path

            if (scriptsInTable.contains(path)) continue

            scriptsInTable.add(path)
            model.addRow(path)
        }
    }

    private fun removeScripts() {
        val selected = table.selectedRows
        if (selected == null || selected.isEmpty()) {
            return
        }
        for ((removedCount, indexToRemove) in selected.withIndex()) {
            val row = indexToRemove - removedCount
            scriptsInTable.removeAt(row)
            model.removeRow(row)
        }
        IdeFocusManager.getGlobalInstance()
            .doWhenFocusSettlesDown { IdeFocusManager.getGlobalInstance().requestFocus(table, true) }
    }
}