/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.PlatformUtils
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.util.ProgressIndicatorUtils.underModalProgress
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm

abstract class ConfigureKotlinInProjectAction : AnAction() {

    abstract fun getApplicableConfigurators(project: Project): Collection<KotlinProjectConfigurator>

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val (modules, configurators) = underModalProgress(project, KotlinJvmBundle.message("lookup.project.configurators.progress.text")) {
            val modules = getConfigurableModules(project)
            if (modules.all(::isModuleConfigured)) {
                return@underModalProgress modules to emptyList<KotlinProjectConfigurator>()
            }
            val configurators = getApplicableConfigurators(project)
            modules to configurators
        }

        if (modules.all(::isModuleConfigured)) {
            Messages.showInfoMessage(KotlinJvmBundle.message("all.modules.with.kotlin.files.are.configured"), e.presentation.text!!)
            return
        }

        when {
            configurators.size == 1 -> configurators.first().configure(project, emptyList())
            configurators.isEmpty() -> Messages.showErrorDialog(
                KotlinJvmBundle.message("there.aren.t.configurators.available"),
                e.presentation.text!!
            )
            else -> {
                val configuratorsPopup =
                    KotlinSetupEnvironmentNotificationProvider.createConfiguratorsPopup(project, configurators.toList())
                configuratorsPopup.showInBestPositionFor(e.dataContext)
            }
        }
    }
}


class ConfigureKotlinJsInProjectAction : ConfigureKotlinInProjectAction() {
    override fun getApplicableConfigurators(project: Project) = getAbleToRunConfigurators(project).filter {
        it.targetPlatform.isJs()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (!PlatformUtils.isIntelliJ() &&
            (project == null || project.allModules().all { it.getBuildSystemType() != BuildSystemType.JPS })
        ) {
            e.presentation.isEnabledAndVisible = false
        }
    }
}

class ConfigureKotlinJavaInProjectAction : ConfigureKotlinInProjectAction() {
    override fun getApplicableConfigurators(project: Project) = getAbleToRunConfigurators(project).filter {
        it.targetPlatform.isJvm()
    }
}