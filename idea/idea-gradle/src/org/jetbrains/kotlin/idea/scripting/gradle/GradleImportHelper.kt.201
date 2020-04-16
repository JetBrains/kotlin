/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.diff.util.DiffUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.testFramework.LightVirtualFileBase
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModelResolver
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.plugins.gradle.service.project.GradlePartialResolverPolicy
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.function.Predicate

fun runPartialGradleImport(project: Project) {
    val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
    val projectSettings = gradleSettings.getLinkedProjectsSettings()
        .filterIsInstance<GradleProjectSettings>()
        .firstOrNull() ?: return

    ExternalSystemUtil.refreshProject(
        projectSettings.externalProjectPath,
        ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            .projectResolverPolicy(
                GradlePartialResolverPolicy(Predicate<GradleProjectResolverExtension?> { it is KotlinDslScriptModelResolver })
            )
    )
}

private var Project.shouldShowLoadConfiguraionsAction: Boolean?
        by UserDataProperty<Project, Boolean>(Key.create("load.script.configuration.action"))


fun showNotificationForProjectImport(project: Project) {
    project.shouldShowLoadConfiguraionsAction = true
}

fun hideNotificationForProjectImport(project: Project): Boolean {
    project.shouldShowLoadConfiguraionsAction = false
    return true
}

class LoadConfigurationAction : AnAction(
    KotlinIdeaGradleBundle.message("action.text.load.script.configurations"),
    KotlinIdeaGradleBundle.message("action.description.load.script.configurations"),
    KotlinIcons.LOAD_SCRIPT_CONFIGURATION
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        runPartialGradleImport(project)
    }

    override fun update(e: AnActionEvent) {
        ensureValidActionVisibility(e)

        e.presentation.description = KotlinIdeaGradleBundle.message("action.description.load.script.configurations")
    }

    private fun ensureValidActionVisibility(e: AnActionEvent) {
        if (e.project?.shouldShowLoadConfiguraionsAction != true) {
            e.presentation.isVisible = false
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        if (DiffUtil.isDiffEditor(editor)) {
            e.presentation.isVisible = false
            return
        }
        e.presentation.isVisible = editor.isScriptEditor()
    }

    private fun Editor.isScriptEditor(): Boolean {
        val project = project ?: return false

        val documentManager = FileDocumentManager.getInstance()
        val virtualFile = documentManager.getFile(document)
        if (virtualFile is LightVirtualFileBase) return false
        if (virtualFile == null || !virtualFile.isValid) return false


        // todo only for gradle script
        return virtualFile.findScriptDefinition(project) != null
    }
}