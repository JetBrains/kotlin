/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script.configuration

import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.ui.HyperlinkLabel
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.StandardIdeScriptDefinition
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinitionAdapterFromNewAPIBase
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate

class MultipleScriptDefinitionsChecker(private val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (file.fileType != KotlinFileType.INSTANCE) return null

        val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return null

        if (!ktFile.isScript()) return null
        if (KotlinScriptingSettings.getInstance(ktFile.project).suppressDefinitionsCheck) return null

        val allApplicableDefinitions = ScriptDefinitionsManager.getInstance(project)
            .getAllDefinitions()
            .filter {
                it !is StandardIdeScriptDefinition &&
                        it.isScript(ktFile.name) &&
                        KotlinScriptingSettings.getInstance(project).isScriptDefinitionEnabled(it)
            }
            .toList()
        if (allApplicableDefinitions.size < 2) return null
        if (areDefinitionsForGradleKts(allApplicableDefinitions)) return null

        return createNotification(
            ktFile,
            allApplicableDefinitions
        )
    }

    private fun areDefinitionsForGradleKts(allApplicableDefinitions: List<KotlinScriptDefinition>): Boolean {
        if (allApplicableDefinitions.size == 2) {
            return (allApplicableDefinitions[0] as? KotlinScriptDefinitionFromAnnotatedTemplate)?.scriptFilePattern?.pattern == "^(settings|.+\\.settings)\\.gradle\\.kts\$"
                    && (allApplicableDefinitions[1] as? KotlinScriptDefinitionFromAnnotatedTemplate)?.scriptFilePattern?.pattern == ".*\\.gradle\\.kts"
        }
        return false
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("MultipleScriptDefinitionsChecker")

        private fun createNotification(psiFile: KtFile, defs: List<KotlinScriptDefinition>): EditorNotificationPanel {
            return EditorNotificationPanel().apply {
                setText("Multiple script definitions are applicable for this script. ${defs.first().name} is used")
                createComponentActionLabel("Show all") {
                    val list = JBPopupFactory.getInstance().createListPopup(
                        object : BaseListPopupStep<KotlinScriptDefinition>(null, defs) {
                            override fun getTextFor(value: KotlinScriptDefinition): String {
                                return when (value) {
                                    is KotlinScriptDefinitionFromAnnotatedTemplate -> {
                                        value.name + " (${value.scriptFilePattern})"
                                    }
                                    is KotlinScriptDefinitionAdapterFromNewAPIBase -> {
                                        value.name + " (${value.fileExtension})"
                                    }
                                    is StandardIdeScriptDefinition -> {
                                        value.name + " (${KotlinParserDefinition.STD_SCRIPT_EXT})"
                                    }
                                    else -> value.name
                                }
                            }
                        }
                    )
                    list.showUnderneathOf(it)
                }

                createComponentActionLabel("Ignore") {
                    KotlinScriptingSettings.getInstance(psiFile.project).suppressDefinitionsCheck = true
                    EditorNotifications.getInstance(psiFile.project).updateAllNotifications()
                }

                createComponentActionLabel("Open Settings") {
                    ShowSettingsUtilImpl.showSettingsDialog(psiFile.project, KotlinScriptingSettingsConfigurable.ID, "")
                }
            }
        }

        private fun EditorNotificationPanel.createComponentActionLabel(labelText: String, callback: (HyperlinkLabel) -> Unit) {
            val label: Ref<HyperlinkLabel> = Ref.create()
            label.set(createActionLabel(labelText) {
                callback(label.get())
            })
        }
    }
}