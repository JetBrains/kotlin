/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.systemIndependentPath
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationCacheScope
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.plugins.gradle.GradleManager
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

open class GradleScriptListener(project: Project) : ScriptChangeListener(project) {

    override fun editorActivated(vFile: VirtualFile, updater: ScriptConfigurationUpdater) {
        if (!isGradleKotlinScript(vFile)) return

        if (useScriptConfigurationFromImportOnly()) {
            // do nothing
        } else {
            val file = getAnalyzableKtFileForScript(vFile) ?: return
            updater.ensureUpToDatedConfigurationSuggested(file)
        }
    }

    override fun documentChanged(vFile: VirtualFile, updater: ScriptConfigurationUpdater) {
        val file = getAnalyzableKtFileForScript(vFile)
        if (file != null) {
            // *.gradle.kts file was changed
            updater.ensureUpToDatedConfigurationSuggested(file)
        }
        project.service<GradleScriptInputsWatcher>().addToStorage(vFile)
    }

    override fun isApplicable(vFile: VirtualFile): Boolean {
        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        if (gradleSettings.getLinkedProjectsSettings().isEmpty()) return false

        val projectSettings = gradleSettings.getLinkedProjectsSettings().filterIsInstance<GradleProjectSettings>().firstOrNull()
            ?: return false

        val affectedFiles = ExternalSystemApiUtil.getAllManagers().filterIsInstance<GradleManager>().firstOrNull()
            ?.getAffectedExternalProjectFiles(projectSettings.externalProjectPath, project)
        return affectedFiles?.any { it.toPath().systemIndependentPath == vFile.path } == true
    }
}