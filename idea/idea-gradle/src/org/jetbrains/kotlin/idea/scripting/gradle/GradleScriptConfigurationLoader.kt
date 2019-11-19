/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

class GradleScriptConfigurationLoader(project: Project) : DefaultScriptConfigurationLoader(project) {
    private val useProjectImport: Boolean
        get() = Registry.`is`("kotlin.gradle.scripts.useIdeaProjectImport", false)

    override fun shouldRunInBackground(scriptDefinition: ScriptDefinition): Boolean {
        return if (useProjectImport) false else super.shouldRunInBackground(scriptDefinition)
    }

    override fun loadDependencies(
        isFirstLoad: Boolean,
        ktFile: KtFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean {
        if (!isGradleKotlinScript(ktFile.originalFile.virtualFile)) return false

        if (useProjectImport) {
            // do nothing, project import notification will be already showed
            // and configuration for gradle build scripts will be saved at the end of import
            // todo: use default configuration loader for out-of-project scripts?

            return true
        } else {
            // Gradle read files from FS
            GlobalScope.launch(EDT(project)) {
                runWriteAction {
                    FileDocumentManager.getInstance().saveAllDocuments()
                }
            }

            return super.loadDependencies(isFirstLoad, ktFile, scriptDefinition, context)
        }
    }

    override fun getInputsStamp(virtualFile: VirtualFile, file: KtFile): CachedConfigurationInputs {
        return getGradleScriptInputsStamp(project, virtualFile, file)
            ?: super.getInputsStamp(virtualFile, file)
    }
}