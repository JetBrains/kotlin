/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater

open class GradleScriptListener(project: Project) : ScriptChangeListener(project) {
    init {
        // start GradleScriptInputsWatcher to track changes in gradle-configuration related files
        project.service<GradleScriptInputsWatcher>().startWatching()
    }

    override fun editorActivated(vFile: VirtualFile, updater: ScriptConfigurationUpdater) {
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
    }

    override fun isApplicable(vFile: VirtualFile): Boolean {
        if (!isGradleKotlinScript(vFile)) return false

        return isInAffectedGradleProjectFiles(project, vFile)
    }
}