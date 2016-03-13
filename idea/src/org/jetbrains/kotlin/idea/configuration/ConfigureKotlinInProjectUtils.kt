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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.configuration.ui.notifications.ConfigureKotlinNotification
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.versions.getKotlinRuntimeMarkerClass
import org.jetbrains.kotlin.utils.ifEmpty

fun isProjectConfigured(project: Project): Boolean {
    val modules = getModulesWithKotlinFiles(project)
    return modules.all { isModuleConfigured(it) }
}

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

    ConfigureKotlinNotificationManager.notify(module.project)
}

fun showConfigureKotlinNotificationIfNeeded(project: Project, excludeModules: List<Module> = emptyList()) {
    ApplicationManager.getApplication().executeOnPooledThread {
        val notificationString = DumbService.getInstance(project).runReadActionInSmartMode(Computable {
            val modules = getModulesWithKotlinFiles(project) - excludeModules
            if (modules.all { isModuleConfigured(it) }) null else ConfigureKotlinNotification.getNotificationString(project, excludeModules)
        })
        if (notificationString != null) {
            ApplicationManager.getApplication().invokeLater {
                ConfigureKotlinNotificationManager.notify(project, ConfigureKotlinNotification(project, excludeModules, notificationString))
            }
        }
    }
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

fun getNonConfiguredModules(project: Project, excludeModules: Collection<Module> = emptyList()): Collection<Module> {
    val modulesWithKotlinFiles = getModulesWithKotlinFiles(project) - excludeModules
    val ableToRunConfigurators = getAbleToRunConfigurators(project)
    return modulesWithKotlinFiles.filter { module ->
        ableToRunConfigurators.any { !it.isConfigured(module) }
    }
}

fun hasKotlinRuntimeInScope(module: Module): Boolean {
    val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
    return getKotlinRuntimeMarkerClass(module.project, scope) != null
}

fun hasKotlinFilesOnlyInTests(module: Module): Boolean {
    return !hasKotlinFilesInSources(module) && FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(true))
}

fun hasKotlinFilesInSources(module: Module): Boolean {
    return FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(false))
}
