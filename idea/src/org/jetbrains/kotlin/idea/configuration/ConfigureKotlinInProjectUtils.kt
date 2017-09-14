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
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.configuration.ui.notifications.ConfigureKotlinNotification
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.util.application.runReadAction
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

val EAP_12_REPOSITORY = RepositoryDescription(
        "bintray.kotlin.eap",
        "Bintray Kotlin 1.2 EAP Repository",
        "http://dl.bintray.com/kotlin/kotlin-eap-1.2",
        "https://bintray.com/kotlin/kotlin-eap-1.2/kotlin/",
        isSnapshot = false)

val MAVEN_CENTRAL = "mavenCentral()"

val JCENTER = "jcenter()"

val KOTLIN_GROUP_ID = "org.jetbrains.kotlin"

fun isRepositoryConfigured(repositoriesBlockText: String): Boolean =
        repositoriesBlockText.contains(MAVEN_CENTRAL) || repositoriesBlockText.contains(JCENTER)

fun DependencyScope.toGradleCompileScope(isAndroidModule: Boolean) = when (this) {
    DependencyScope.COMPILE -> "compile"
    // TODO: We should add testCompile or androidTestCompile
    DependencyScope.TEST -> if (isAndroidModule) "compile" else "testCompile"
    DependencyScope.RUNTIME -> "runtime"
    DependencyScope.PROVIDED -> "compile"
    else -> "compile"
}

fun RepositoryDescription.toGroovyRepositorySnippet() = "maven {\nurl '$url'\n}"

fun RepositoryDescription.toKotlinRepositorySnippet() = "maven {\nsetUrl(\"$url\")\n}"

fun getRepositoryForVersion(version: String): RepositoryDescription? = when {
    isSnapshot(version) -> SNAPSHOT_REPOSITORY
    useEapRepository(2, version) -> EAP_12_REPOSITORY
    useEapRepository(1, version) -> EAP_11_REPOSITORY
    isEap(version) -> EAP_REPOSITORY
    else -> null
}

fun isModuleConfigured(moduleSourceRootGroup: ModuleSourceRootGroup): Boolean {
    return allConfigurators().any {
        it.getStatus(moduleSourceRootGroup) == ConfigureKotlinStatus.CONFIGURED
    }
}

fun getModulesWithKotlinFiles(project: Project): Collection<Module> {
    if (!runReadAction {
        !project.isDisposed && FileTypeIndex.containsFileOfType (KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project))
    }) {
        return emptyList()
    }

    return project.allModules()
            .filter { module ->
                runReadAction {
                    !project.isDisposed && FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(true))
                }
            }
}

fun getConfigurableModulesWithKotlinFiles(project: Project): List<ModuleSourceRootGroup> {
    val modules = getModulesWithKotlinFiles(project)
    if (modules.isEmpty()) return emptyList()

    return ModuleSourceRootMap(project).groupByBaseModules(modules)
}

fun showConfigureKotlinNotificationIfNeeded(module: Module) {
    val moduleGroup = ModuleSourceRootMap(module.project).toModuleGroup(module)
    if (isModuleConfigured(moduleGroup)) return

    ConfigureKotlinNotificationManager.notify(module.project)
}

fun showConfigureKotlinNotificationIfNeeded(project: Project, excludeModules: List<Module> = emptyList()) {
    val notificationString = DumbService.getInstance(project).runReadActionInSmartMode(Computable {
        val modules = getConfigurableModulesWithKotlinFiles(project).exclude(excludeModules)
        if (modules.all(::isModuleConfigured))
            null
        else
            ConfigureKotlinNotification.getNotificationString(project, excludeModules)
    })

    if (notificationString != null) {
        ApplicationManager.getApplication().invokeLater {
            ConfigureKotlinNotificationManager.notify(project, ConfigureKotlinNotification(project, excludeModules, notificationString))
        }
    }
}

fun getAbleToRunConfigurators(project: Project): Collection<KotlinProjectConfigurator> {
    val modules = getConfigurableModules(project)

    return allConfigurators().filter { configurator ->
        modules.any { configurator.getStatus(it) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
    }
}

fun getConfigurableModules(project: Project): List<ModuleSourceRootGroup> {
    return getConfigurableModulesWithKotlinFiles(project).ifEmpty {
        ModuleSourceRootMap(project).groupByBaseModules(project.allModules())
    }
}

fun getAbleToRunConfigurators(module: Module): Collection<KotlinProjectConfigurator> {
    val moduleGroup = module.toModuleGroup()
    return allConfigurators().filter {
        it.getStatus(moduleGroup) == ConfigureKotlinStatus.CAN_BE_CONFIGURED
    }
}

fun getConfiguratorByName(name: String): KotlinProjectConfigurator? {
    return allConfigurators().firstOrNull { it.name == name }
}

fun allConfigurators() = Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)

fun getCanBeConfiguredModules(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    return ModuleSourceRootMap(project).groupByBaseModules(project.allModules())
            .filter { configurator.canConfigure(it) }
            .map { it.baseModule }
}

private fun KotlinProjectConfigurator.canConfigure(moduleSourceRootGroup: ModuleSourceRootGroup) =
        getStatus(moduleSourceRootGroup) == ConfigureKotlinStatus.CAN_BE_CONFIGURED &&
        (allConfigurators().toList() - this).none { it.getStatus(moduleSourceRootGroup) == ConfigureKotlinStatus.CONFIGURED }

fun getCanBeConfiguredModulesWithKotlinFiles(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    val modules = getConfigurableModulesWithKotlinFiles(project)
    return modules.filter { configurator.getStatus(it) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }.map { it.baseModule }
}

fun getCanBeConfiguredModulesWithKotlinFiles(project: Project, excludeModules: Collection<Module> = emptyList()): Collection<Module> {
    val modulesWithKotlinFiles = getConfigurableModulesWithKotlinFiles(project).exclude(excludeModules)
    val configurators = allConfigurators()
    return modulesWithKotlinFiles.filter { moduleSourceRootGroup ->
        configurators.any { it.getStatus(moduleSourceRootGroup) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }
    }.map { it.baseModule }
}

fun findApplicableConfigurator(module: Module): KotlinProjectConfigurator {
    val moduleGroup = module.toModuleGroup()
    return allConfigurators().find { it.getStatus(moduleGroup) != ConfigureKotlinStatus.NON_APPLICABLE }
           ?: KotlinJavaModuleConfigurator.instance
}

fun hasAnyKotlinRuntimeInScope(module: Module): Boolean {
    return runReadAction {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
        getKotlinJvmRuntimeMarkerClass(module.project, scope) != null ||
        hasKotlinJsKjsmFile(module.project, LibraryKindSearchScope(module, scope, JSLibraryKind) ) ||
        hasKotlinCommonRuntimeInScope(scope)
    }
}

fun hasKotlinJvmRuntimeInScope(module: Module): Boolean {
    return runReadAction {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
        getKotlinJvmRuntimeMarkerClass(module.project, scope) != null
    }
}

fun hasKotlinJsRuntimeInScope(module: Module): Boolean {
    return runReadAction {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
        hasKotlinJsKjsmFile(module.project, LibraryKindSearchScope(module, scope, JSLibraryKind))
    }
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

fun useEapRepository(minorKotlinVersion: Int, version: String): Boolean {
    return Regex("1\\.$minorKotlinVersion(\\.\\d)?-[A-Za-z][A-Za-z0-9-]*").matches(version) &&
           !version.startsWith("1.$minorKotlinVersion.0-dev")
}

private class LibraryKindSearchScope(val module: Module,
                                     val baseScope: GlobalSearchScope,
                                     val libraryKind: PersistentLibraryKind<*>
) : DelegatingGlobalSearchScope(baseScope) {
    override fun contains(file: VirtualFile): Boolean {
        if (!super.contains(file)) return false
        val orderEntry = ModuleRootManager.getInstance(module).fileIndex.getOrderEntryForFile(file)
        if (orderEntry is LibraryOrderEntry) {
            return (orderEntry.library as LibraryEx).kind == libraryKind
        }
        return true
    }
}
