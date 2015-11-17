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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.utils.ifEmpty

fun isProjectConfigured(project: Project): Boolean {
    val modules = getModulesWithKotlinFiles(project)
    return modules.all { isModuleConfigured(it) }
}

fun Project.allModules() = ModuleManager.getInstance(this).modules.toList()

fun isModuleConfigured(module: Module): Boolean {
    val configurators = getApplicableConfigurators(module)
    return configurators.any { it.isConfigured(module) }
}

fun getModulesWithKotlinFiles(project: Project): Collection<Module> {
    if (project.isDisposed) {
        return emptyList()
    }

    if (!FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project))) {
        return emptyList()
    }

    return project.allModules().filter { module ->
        FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(true))
    }
}

fun showConfigureKotlinNotificationIfNeeded(module: Module) {
    if (isModuleConfigured(module)) return

    showConfigureKotlinNotification(module.project)
}

fun showConfigureKotlinNotificationIfNeeded(project: Project) {
    if (isProjectConfigured(project)) return

    showConfigureKotlinNotification(project)
}

private fun showConfigureKotlinNotification(project: Project) {
    ConfigureKotlinNotificationManager.notify(project)
}

fun getAbleToRunConfigurators(project: Project): Collection<KotlinProjectConfigurator> {
    val modules = getModulesWithKotlinFiles(project).ifEmpty { project.allModules() }

    return Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME).filter { configurator ->
        modules.any { module -> configurator.isApplicable(module) && !configurator.isConfigured(module) }
    }
}

fun getApplicableConfigurators(module: Module): Collection<KotlinProjectConfigurator> {
    return Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME).filter { it.isApplicable(module) }
}

fun getConfiguratorByName(name: String): KotlinProjectConfigurator? {
    return Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME).firstOrNull { it.name == name }
}

fun getNonConfiguredModules(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    return project.allModules().filter { module -> configurator.isApplicable(module) && !configurator.isConfigured(module) }
}

fun getNonConfiguredModulesWithKotlinFiles(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    val modules = getModulesWithKotlinFiles(project)
    return modules.filter { module -> configurator.isApplicable(module) && !configurator.isConfigured(module) }
}

fun getNonConfiguredModules(project: Project): Collection<Module> {
    val modulesWithKotlinFiles = getModulesWithKotlinFiles(project)
    return modulesWithKotlinFiles.filter { module ->
        getAbleToRunConfigurators(project).any { !it.isConfigured(module) }
    }
}

fun showInfoNotification(project: Project, message: String) {
    Notifications.Bus.notify(Notification("Configure Kotlin: info notification", "Configure Kotlin", message, NotificationType.INFORMATION), project)
}
