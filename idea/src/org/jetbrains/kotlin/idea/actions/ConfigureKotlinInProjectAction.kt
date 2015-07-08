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
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils
import org.jetbrains.kotlin.idea.project.TargetPlatform

public abstract class ConfigureKotlinInProjectAction : AnAction() {

    abstract fun getAbleToRunConfigurators(project: Project): Collection<KotlinProjectConfigurator>

    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext())
        if (project == null) return

        if (ConfigureKotlinInProjectUtils.isProjectConfigured(project)) {
            Messages.showInfoMessage("All modules with kotlin files are configured", e.getPresentation().getText()!!)
            return
        }

        val configurators = getAbleToRunConfigurators(project)

        when {
            configurators.size() == 1 -> configurators.first().configure(project)
            configurators.isEmpty() -> Messages.showErrorDialog("There aren't configurators available", e.getPresentation().getText()!!)
            else -> {
                Messages.showErrorDialog("More than one configurator is available", e.getPresentation().getText()!!)
                ConfigureKotlinInProjectUtils.showConfigureKotlinNotificationIfNeeded(project)
            }
        }
    }
}

public class ConfigureKotlinJsInProjectAction: ConfigureKotlinInProjectAction() {
    override fun getAbleToRunConfigurators(project: Project) = ConfigureKotlinInProjectUtils.getAbleToRunConfigurators(project).filter {
        it.getTargetPlatform() == TargetPlatform.JS
    }
}

public class ConfigureKotlinJavaInProjectAction: ConfigureKotlinInProjectAction() {
    override fun getAbleToRunConfigurators(project: Project) = ConfigureKotlinInProjectUtils.getAbleToRunConfigurators(project).filter {
        it.getTargetPlatform() == TargetPlatform.JVM
    }
}