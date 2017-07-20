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

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.PlatformUtils
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

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
        it.targetPlatform == JsPlatform
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (!PlatformUtils.isIntelliJ() && (project == null || project.allModules().all(KotlinPluginUtil::isAndroidGradleModule))) {
            e.presentation.isEnabledAndVisible = false
        }
    }
}

class ConfigureKotlinJavaInProjectAction: ConfigureKotlinInProjectAction() {
    override fun getApplicableConfigurators(project: Project) = getAbleToRunConfigurators(project).filter {
        it.targetPlatform is JvmPlatform
    }
}