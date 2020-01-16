/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager

class GradleScriptOutOfSourceNotificationProvider(private val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!isGradleKotlinScript(file)) return null
        if (file.fileType != KotlinFileType.INSTANCE) return null

        if (isInAffectedGradleProjectFiles(project, file.path)) return null

        return EditorNotificationPanel().apply {
            text("This script isn't related to any Gradle Project. To update its highlighting, please, load its script configuration.")
            createActionLabel("Load configuration") {
                ScriptConfigurationManager.getInstance(project).forceReloadConfiguration(file, loaderForOutOfProjectScripts)
            }
        }
    }

    private val loaderForOutOfProjectScripts by lazy {
        GradleScriptConfigurationLoaderForOutOfProjectScripts(project)
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("GradleScriptOutOfSourceNotification")
    }
}