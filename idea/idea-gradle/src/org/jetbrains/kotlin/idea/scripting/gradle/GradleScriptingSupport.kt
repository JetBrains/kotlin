/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupport
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

class GradleScriptingSupport(
    val project: Project,
    val buildRoot: VirtualFile,
    val context: GradleKtsContext,
    val configuration: Configuration
) : ScriptingSupport() {

    override fun recreateRootsCache() = GradleClassRootsCache(project, context, configuration)

    fun isConfigurationOutOfDate(file: VirtualFile): Boolean {
        val scriptModel = configuration.scriptModel(file) ?: return false
        return !scriptModel.inputs.isUpToDate(project, file)
    }

    override fun clearCaches() {
        // todo: should clear up to date
    }

    override fun hasCachedConfiguration(file: KtFile): Boolean =
        configuration.scriptModel(file.originalFile.virtualFile) != null

    // TODO: can be true during import
    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean = false

    override fun getOrLoadConfiguration(virtualFile: VirtualFile, preloadedKtFile: KtFile?): ScriptCompilationConfigurationWrapper? {
        return classpathRoots.getScriptConfiguration(virtualFile)
    }

    override val updater: ScriptConfigurationUpdater
        get() = object : ScriptConfigurationUpdater {
            override fun ensureUpToDatedConfigurationSuggested(file: KtFile) {
                // do nothing for gradle scripts
            }

            // unused symbol inspection should not initiate loading
            override fun ensureConfigurationUpToDate(files: List<KtFile>): Boolean = true

            override fun suggestToUpdateConfigurationIfOutOfDate(file: KtFile) {
                val vFile = file.originalFile.virtualFile

                if (isConfigurationOutOfDate(vFile)) {
                    showNotificationForProjectImport(project)
                } else {
                    hideNotificationForProjectImport(project)
                }
            }
        }
}