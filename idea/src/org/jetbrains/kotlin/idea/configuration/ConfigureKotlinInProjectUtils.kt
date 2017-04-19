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
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
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

data class RepositoryDescription(val id: String, val name: String, val url: String, val bintrayUrl: String?, val isSnapshot: Boolean)

val SNAPSHOT_REPOSITORY = RepositoryDescription(
        "sonatype.oss.snapshots",
        "Sonatype OSS Snapshot Repository",
        "http://oss.sonatype.org/content/repositories/snapshots",
        null,
        isSnapshot = true)

val EAP_REPOSITORY = RepositoryDescription(
        "bintray.kotlin.eap",
        "Bintray Kotlin EAP Repository",
        "http://dl.bintray.com/kotlin/kotlin-eap",
        "https://bintray.com/kotlin/kotlin-eap/kotlin/",
        isSnapshot = false)

val EAP_11_REPOSITORY = RepositoryDescription(
        "bintray.kotlin.eap",
        "Bintray Kotlin 1.1 EAP Repository",
        "http://dl.bintray.com/kotlin/kotlin-eap-1.1",
        "https://bintray.com/kotlin/kotlin-eap-1.1/kotlin/",
        isSnapshot = false)

fun RepositoryDescription.toRepositorySnippet() = "maven {\nurl '$url'\n}"

fun getRepositoryForVersion(version: String): RepositoryDescription? = when {
    isSnapshot(version) -> SNAPSHOT_REPOSITORY
    useEap11Repository(version) -> EAP_11_REPOSITORY
    isEap(version) -> EAP_REPOSITORY
    else -> null
}

fun isModuleConfigured(module: Module): Boolean {
    return allConfigurators().any {
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

    return project.allModules()
            .filter { module ->
                FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(true))
            }
}

fun getConfigurableModulesWithKotlinFiles(project: Project): Collection<Module> {
    val modules = getModulesWithKotlinFiles(project)
    if (modules.isEmpty()) return modules

    val pathMap = ModuleManager.getInstance(project).modules.asList().buildExternalPathMap()
    return modules.mapTo(HashSet()) { module ->
        val externalPath = module.externalProjectPath
        if (externalPath == null) module else (pathMap[externalPath] ?: module)
    }
}

fun showConfigureKotlinNotificationIfNeeded(module: Module) {
    if (isModuleConfigured(module)) return

    ConfigureKotlinNotificationManager.notify(module.project)
}

fun showConfigureKotlinNotificationIfNeeded(project: Project, excludeModules: List<Module> = emptyList()) {
    ApplicationManager.getApplication().executeOnPooledThread {
        val notificationString = DumbService.getInstance(project).runReadActionInSmartMode(Computable {
            val modules = getConfigurableModulesWithKotlinFiles(project) - excludeModules
            if (modules.all(::isModuleConfigured)) null else ConfigureKotlinNotification.getNotificationString(project, excludeModules)
        })
        if (notificationString != null) {
            ApplicationManager.getApplication().invokeLater {
                ConfigureKotlinNotificationManager.notify(project, ConfigureKotlinNotification(project, excludeModules, notificationString))
            }
        }
    }
}

fun getAbleToRunConfigurators(project: Project): Collection<KotlinProjectConfigurator> {
    val modules = getConfigurableModulesWithKotlinFiles(project).ifEmpty { project.allModules() }

    return allConfigurators().filter { configurator ->
        modules.any { module -> configurator.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
    }
}

fun getAbleToRunConfigurators(module: Module): Collection<KotlinProjectConfigurator> {
    return allConfigurators().filter { it.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
}

fun getConfiguratorByName(name: String): KotlinProjectConfigurator? {
    return allConfigurators().firstOrNull { it.name == name }
}

fun allConfigurators() = Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)

fun getNonConfiguredModules(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    return project.allModules()
            .filter { module -> configurator.canConfigure(module) }
            .excludeSourceRootModules()
}

private fun KotlinProjectConfigurator.canConfigure(module: Module) =
        getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED &&
        (allConfigurators().toList() - this).none { it.getStatus(module) == ConfigureKotlinStatus.CONFIGURED }

fun Collection<Module>.excludeSourceRootModules(): List<Module> {
    val pathMap = buildExternalPathMap()
    return filter { it.externalProjectId == null || it.externalProjectPath == null } + pathMap.values
}

fun Collection<Module>.buildExternalPathMap(): Map<String, Module> {
    val pathMap = mutableMapOf<String, Module>()
    for (module in this) {
        val externalId = module.externalProjectId
        val externalPath = module.externalProjectPath
        if (externalId != null && externalPath != null) {
            val previousModule = pathMap[externalPath]
            // the module without the source root suffix will have the shortest name
            if (previousModule == null || isSourceRootPrefix(externalId, previousModule.externalProjectId!!)) {
                pathMap[externalPath] = module
            }
        }
    }
    return pathMap
}

private fun isSourceRootPrefix(externalId: String, previousModuleExternalId: String)
        = externalId.length < previousModuleExternalId.length && previousModuleExternalId.startsWith(externalId)

val Module.externalProjectId: String?
    get() = ExternalSystemApiUtil.getExternalProjectId(this)

val Module.externalProjectPath: String?
    get() = ExternalSystemApiUtil.getExternalProjectPath(this)

fun getNonConfiguredModulesWithKotlinFiles(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    val modules = getConfigurableModulesWithKotlinFiles(project)
    return modules.filter { module -> configurator.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
}

fun getNonConfiguredModulesWithKotlinFiles(project: Project, excludeModules: Collection<Module> = emptyList()): Collection<Module> {
    val modulesWithKotlinFiles = getConfigurableModulesWithKotlinFiles(project) - excludeModules
    val configurators = allConfigurators()
    return modulesWithKotlinFiles.filter { module ->
        configurators.any { it.getStatus(module) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
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
    return version.contains("SNAPSHOT", ignoreCase = true)
}

fun isEap(version: String): Boolean {
    return version.contains("rc") || version.contains("eap")
}

fun useEap11Repository(version: String): Boolean {
    return Regex("1\\.1(\\.\\d)?-[A-Za-z][A-Za-z0-9-]*").matches(version) && !version.startsWith("1.1.0-dev")
}
