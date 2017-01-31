/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.configuration.ui.notifications.ConfigureKotlinNotification
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.versions.getKotlinJvmRuntimeMarkerClass
import org.jetbrains.kotlin.idea.versions.hasKotlinJsKjsmFile
import org.jetbrains.kotlin.idea.vfilefinder.IDEVirtualFileFinder
import org.jetbrains.kotlin.utils.ifEmpty

data class RepositoryDescription(val id: String, val name: String, val url: String, val isSnapshot: Boolean)

@JvmField
val SNAPSHOT_REPOSITORY = RepositoryDescription(
        "sonatype.oss.snapshots",
        "Sonatype OSS Snapshot Repository",
        "http://oss.sonatype.org/content/repositories/snapshots",
        isSnapshot = true)

@JvmField
val EAP_REPOSITORY = RepositoryDescription(
        "bintray.kotlin.eap",
        "Bintray Kotlin EAP Repository",
        "http://dl.bintray.com/kotlin/kotlin-eap",
        isSnapshot = false)

@JvmField
val EAP_11_REPOSITORY = RepositoryDescription(
        "bintray.kotlin.eap",
        "Bintray Kotlin 1.1 EAP Repository",
        "http://dl.bintray.com/kotlin/kotlin-eap-1.1",
        isSnapshot = false)

fun isModuleConfigured(module: Module): Boolean {
    return Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME).any {
        it.getStatus(module) == ConfigureKotlinStatus.CONFIGURED
    }
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
        modules.any { module -> configurator.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
    }
}

fun getAbleToRunConfigurators(module: Module): Collection<KotlinProjectConfigurator> {
    return Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME).filter { it.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
}

fun getConfiguratorByName(name: String): KotlinProjectConfigurator? {
    return Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME).firstOrNull { it.name == name }
}

fun getNonConfiguredModules(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    return project.allModules().filter { module -> configurator.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
}

fun getNonConfiguredModulesWithKotlinFiles(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    val modules = getModulesWithKotlinFiles(project)
    return modules.filter { module -> configurator.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
}

fun getNonConfiguredModules(project: Project, excludeModules: Collection<Module> = emptyList()): Collection<Module> {
    val modulesWithKotlinFiles = getModulesWithKotlinFiles(project) - excludeModules
    val ableToRunConfigurators = getAbleToRunConfigurators(project)
    return modulesWithKotlinFiles.filter { module ->
        ableToRunConfigurators.any { it.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
    }
}

fun hasAnyKotlinRuntimeInScope(module: Module): Boolean {
    val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
    return getKotlinJvmRuntimeMarkerClass(module.project, scope) != null ||
           hasKotlinJsKjsmFile(module.project, scope) ||
           hasKotlinCommonRuntimeInScope(scope)
}

fun hasKotlinJvmRuntimeInScope(module: Module): Boolean {
    val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
    return getKotlinJvmRuntimeMarkerClass(module.project, scope) != null
}

fun hasKotlinJsRuntimeInScope(module: Module): Boolean {
    val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
    return hasKotlinJsKjsmFile(module.project, scope)
}

fun hasKotlinCommonRuntimeInScope(scope: GlobalSearchScope): Boolean {
    return IDEVirtualFileFinder(scope).hasMetadataPackage(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME)
}

fun hasKotlinFilesOnlyInTests(module: Module): Boolean {
    return !hasKotlinFilesInSources(module) && FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(true))
}

fun hasKotlinFilesInSources(module: Module): Boolean {
    return FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(false))
}

fun isSnapshot(version: String): Boolean {
    return version.contains("SNAPSHOT")
}

fun isEap(version: String): Boolean {
    return version.contains("rc") || version.contains("eap")
}

fun useEap11Repository(version: String): Boolean {
    return Regex("1\\.1(\\.\\d)?-[A-Za-z][A-Za-z0-9-]*").matches(version) && !version.startsWith("1.1.0-dev")
}
