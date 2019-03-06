/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.PlatformUtils
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.resolve.isJs
import org.jetbrains.kotlin.resolve.isJvm

abstract class ConfigureKotlinInProjectAction : AnAction() {

    abstract fun getApplicableConfigurators(project: Project): Collection<KotlinProjectConfigurator>

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val modules = getConfigurableModules(project)
        if (modules.all(::isModuleConfigured)) {
            Messages.showInfoMessage("All modules with Kotlin files are configured", e.presentation.text!!)
            return
        }

        val configurators = getApplicableConfigurators(project)

        when {
            configurators.size == 1 -> configurators.first().configure(project, emptyList())
            configurators.isEmpty() -> Messages.showErrorDialog("There aren't configurators available", e.presentation.text!!)
            else -> {
                val configuratorsPopup = KotlinSetupEnvironmentNotificationProvider.createConfiguratorsPopup(project, configurators.toList())
                configuratorsPopup.showInBestPositionFor(e.dataContext)
            }
        }
    }
}


class ConfigureKotlinJsInProjectAction: ConfigureKotlinInProjectAction() {
    override fun getApplicableConfigurators(project: Project) = getAbleToRunConfigurators(project).filter {
        it.targetPlatform.isJs()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (!PlatformUtils.isIntelliJ() && (project == null || project.allModules().all { it.getBuildSystemType() != BuildSystemType.JPS })) {
            e.presentation.isEnabledAndVisible = false
        }
    }
}

class ConfigureKotlinJavaInProjectAction: ConfigureKotlinInProjectAction() {
    override fun getApplicableConfigurators(project: Project) = getAbleToRunConfigurators(project).filter {
        it.targetPlatform.isJvm()
    }
}