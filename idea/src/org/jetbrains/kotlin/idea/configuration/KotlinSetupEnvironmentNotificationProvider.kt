/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ProjectTopics
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.roots.ModuleRootAdapter
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.versions.UnsupportedAbiVersionNotificationPanelProvider
import org.jetbrains.kotlin.idea.versions.createComponentActionLabel

// Code is partially copied from com.intellij.codeInsight.daemon.impl.SetupSDKNotificationProvider
class KotlinSetupEnvironmentNotificationProvider(
        private val myProject: Project,
        notifications: EditorNotifications) : EditorNotifications.Provider<EditorNotificationPanel>() {

    init {
        myProject.messageBus.connect(myProject).subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootAdapter() {
            override fun rootsChanged(event: ModuleRootEvent?) {
                notifications.updateAllNotifications()
            }
        })
    }

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (file.fileType != KotlinFileType.INSTANCE) {
            return null
        }

        val psiFile = PsiManager.getInstance(myProject).findFile(file) ?: return null
        if (psiFile.language !== KotlinLanguage.INSTANCE) {
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(psiFile) ?: return null
        if (!ModuleRootManager.getInstance(module).fileIndex.isInSourceContent(file)) {
            return null
        }

        if (ModuleRootManager.getInstance(module).sdk == null) {
            return createSetupSdkPanel(myProject, psiFile)
        }

        if (!isModuleConfigured(module) && UnsupportedAbiVersionNotificationPanelProvider.collectBadRoots(module).isEmpty()) {
            return createKotlinNotConfiguredPanel(module)
        }

        return null
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("Setup SDK")

        private fun createSetupSdkPanel(project: Project, file: PsiFile): EditorNotificationPanel {
            return EditorNotificationPanel().apply {
                setText(ProjectBundle.message("project.sdk.not.defined"))
                createActionLabel(ProjectBundle.message("project.sdk.setup")) {
                    ProjectSettingsService.getInstance(project).chooseAndSetSdk() ?: return@createActionLabel

                    runWriteAction {
                        val module = ModuleUtilCore.findModuleForPsiElement(file)
                        if (module != null) {
                            ModuleRootModificationUtil.setSdkInherited(module)
                        }
                    }
                }
            }
        }

        private fun createKotlinNotConfiguredPanel(module: Module): EditorNotificationPanel {
            return EditorNotificationPanel().apply {
                setText("Kotlin not configured")
                val configurators = getApplicableConfigurators(module).toList()
                if (!configurators.isEmpty()) {
                    createComponentActionLabel("Configure") { label ->
                        val step = object : BaseListPopupStep<KotlinProjectConfigurator>("Choose Configurator", configurators) {
                            override fun getTextFor(value: KotlinProjectConfigurator?): String {
                                return value?.presentableText ?: "<none>"
                            }

                            override fun onChosen(selectedValue: KotlinProjectConfigurator?, finalChoice: Boolean): PopupStep<*>? {
                                selectedValue?.let {
                                    it.configure(module.project, emptyList())
                                }
                                return null
                            }
                        }
                        JBPopupFactory.getInstance().createListPopup(step).showUnderneathOf(label)
                    }
                }
            }
        }
    }
}
