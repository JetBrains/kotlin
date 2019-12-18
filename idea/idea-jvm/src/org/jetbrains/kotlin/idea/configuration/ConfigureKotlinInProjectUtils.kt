/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.getKotlinJvmRuntimeMarkerClass
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.effectiveKind
import org.jetbrains.kotlin.idea.quickfix.KotlinAddRequiredModuleFix
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.findFirstPsiJavaModule
import org.jetbrains.kotlin.idea.util.isDev
import org.jetbrains.kotlin.idea.util.isEap
import org.jetbrains.kotlin.idea.util.isSnapshot
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.jetbrains.kotlin.idea.util.projectStructure.version
import org.jetbrains.kotlin.idea.versions.SuppressNotificationState
import org.jetbrains.kotlin.idea.versions.hasKotlinJsKjsmFile
import org.jetbrains.kotlin.idea.vfilefinder.IDEVirtualFileFinder
import org.jetbrains.kotlin.resolve.jvm.modules.KOTLIN_STDLIB_MODULE_NAME
import org.jetbrains.kotlin.utils.ifEmpty

data class RepositoryDescription(val id: String, val name: String, val url: String, val bintrayUrl: String?, val isSnapshot: Boolean)

const val LAST_SNAPSHOT_VERSION = "1.4-SNAPSHOT"

val SNAPSHOT_REPOSITORY = RepositoryDescription(
    "sonatype.oss.snapshots",
    "Sonatype OSS Snapshot Repository",
    "https://oss.sonatype.org/content/repositories/snapshots",
    null,
    isSnapshot = true
)

val EAP_REPOSITORY = RepositoryDescription(
    "bintray.kotlin.eap",
    "Bintray Kotlin EAP Repository",
    "https://dl.bintray.com/kotlin/kotlin-eap",
    "https://bintray.com/kotlin/kotlin-eap/kotlin/",
    isSnapshot = false
)

val DEFAULT_GRADLE_PLUGIN_REPOSITORY = RepositoryDescription(
    "default.gradle.plugins",
    "Default Gradle Plugin Repository",
    "https://plugins.gradle.org/m2/",
    null,
    isSnapshot = false
)

fun devRepository(version: String) = RepositoryDescription(
    "teamcity.kotlin.dev",
    "Teamcity Repository of Kotlin Development Builds",
    "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Compiler),number:$version,branch:(default:any)/artifacts/content/maven",
    null,
    isSnapshot = false
)

val MAVEN_CENTRAL = "mavenCentral()"

val JCENTER = "jcenter()"

val KOTLIN_GROUP_ID = "org.jetbrains.kotlin"

fun isRepositoryConfigured(repositoriesBlockText: String): Boolean =
    repositoriesBlockText.contains(MAVEN_CENTRAL) || repositoriesBlockText.contains(JCENTER)

fun DependencyScope.toGradleCompileScope(isAndroidModule: Boolean) = when (this) {
    DependencyScope.COMPILE -> "implementation"
    // TODO: We should add testCompile or androidTestCompile
    DependencyScope.TEST -> if (isAndroidModule) "implementation" else "testImplementation"
    DependencyScope.RUNTIME -> "runtime"
    DependencyScope.PROVIDED -> "implementation"
    else -> "implementation"
}

fun RepositoryDescription.toGroovyRepositorySnippet() = "maven { url '$url' }"

fun RepositoryDescription.toKotlinRepositorySnippet() = "maven { setUrl(\"$url\") }"

fun getRepositoryForVersion(version: String): RepositoryDescription? = when {
    isSnapshot(version) -> SNAPSHOT_REPOSITORY
    isEap(version) -> EAP_REPOSITORY
    isDev(version) -> devRepository(version)
    else -> null
}

fun isModuleConfigured(moduleSourceRootGroup: ModuleSourceRootGroup): Boolean {
    return allConfigurators().any {
        it.getStatus(moduleSourceRootGroup) == ConfigureKotlinStatus.CONFIGURED
    }
}

/**
 * Returns a list of modules which contain sources in Kotlin.
 * Note that this method is expensive and should not be called more often than strictly necessary.
 */
fun getModulesWithKotlinFiles(project: Project): Collection<Module> {
    if (!runReadAction {
            !project.isDisposed &&
                    FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project))
        }) {
        return emptyList()
    }

    return project.allModules()
        .filter { module ->
            runReadAction {
                !project.isDisposed && !module.isDisposed
                        && FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, module.getModuleScope(true))
            }
        }
}

/**
 * Returns a list of modules which contain sources in Kotlin, grouped by base module.
 * Note that this method is expensive and should not be called more often than strictly necessary.
 */
fun getConfigurableModulesWithKotlinFiles(project: Project): List<ModuleSourceRootGroup> {
    val modules = getModulesWithKotlinFiles(project)
    if (modules.isEmpty()) return emptyList()

    return ModuleSourceRootMap(project).groupByBaseModules(modules)
}

fun showConfigureKotlinNotificationIfNeeded(module: Module) {
    val moduleGroup = module.toModuleGroup()
    if (!isNotConfiguredNotificationRequired(moduleGroup)) return

    ConfigureKotlinNotificationManager.notify(module.project)
}

fun isNotConfiguredNotificationRequired(moduleGroup: ModuleSourceRootGroup): Boolean {
    return !SuppressNotificationState.isKotlinNotConfiguredSuppressed(moduleGroup) && !isModuleConfigured(moduleGroup)
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

fun allConfigurators(): Array<KotlinProjectConfigurator> {
    @Suppress("DEPRECATION")
    return Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME)
}

fun getCanBeConfiguredModules(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    return ModuleSourceRootMap(project).groupByBaseModules(project.allModules())
        .filter { configurator.canConfigure(it) }
        .map { it.baseModule }
}

private fun KotlinProjectConfigurator.canConfigure(moduleSourceRootGroup: ModuleSourceRootGroup) =
    getStatus(moduleSourceRootGroup) == ConfigureKotlinStatus.CAN_BE_CONFIGURED &&
            (allConfigurators().toList() - this).none { it.getStatus(moduleSourceRootGroup) == ConfigureKotlinStatus.CONFIGURED }

/**
 * Returns a list of modules which contain sources in Kotlin and for which it's possible to run the given configurator.
 * Note that this method is expensive and should not be called more often than strictly necessary.
 */
fun getCanBeConfiguredModulesWithKotlinFiles(project: Project, configurator: KotlinProjectConfigurator): List<Module> {
    val modules = getConfigurableModulesWithKotlinFiles(project)
    return modules.filter { configurator.getStatus(it) == ConfigureKotlinStatus.CAN_BE_CONFIGURED }.map { it.baseModule }
}

fun getConfigurationPossibilitiesForConfigureNotification(
    project: Project,
    excludeModules: Collection<Module> = emptyList()
): Pair<Collection<ModuleSourceRootGroup>, Collection<KotlinProjectConfigurator>> {
    val modulesWithKotlinFiles = getConfigurableModulesWithKotlinFiles(project).exclude(excludeModules)
    val configurators = allConfigurators()

    val runnableConfigurators = mutableSetOf<KotlinProjectConfigurator>()
    val configurableModules = mutableListOf<ModuleSourceRootGroup>()

    // We need to return all modules for which at least one configurator is applicable, as well as all configurators which
    // are applicable for at least one module. At the same time we want to call getStatus() only once for each module/configurator pair.
    for (moduleSourceRootGroup in modulesWithKotlinFiles) {
        var moduleCanBeConfigured = false
        var moduleAlreadyConfigured = false
        for (configurator in configurators) {
            if (moduleCanBeConfigured && configurator in runnableConfigurators) continue
            when (configurator.getStatus(moduleSourceRootGroup)) {
                ConfigureKotlinStatus.CAN_BE_CONFIGURED -> {
                    moduleCanBeConfigured = true
                    runnableConfigurators.add(configurator)
                }
                ConfigureKotlinStatus.CONFIGURED -> moduleAlreadyConfigured = true
                else -> {
                }
            }
        }
        if (moduleCanBeConfigured && !moduleAlreadyConfigured && !SuppressNotificationState.isKotlinNotConfiguredSuppressed(
                moduleSourceRootGroup
            )
        )
            configurableModules.add(moduleSourceRootGroup)
    }

    return configurableModules to runnableConfigurators
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
                hasKotlinJsKjsmFile(module.project, LibraryKindSearchScope(module, scope, JSLibraryKind)) ||
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

class LibraryKindSearchScope(
    val module: Module,
    baseScope: GlobalSearchScope,
    val libraryKind: PersistentLibraryKind<*>
) : DelegatingGlobalSearchScope(baseScope) {
    override fun contains(file: VirtualFile): Boolean {
        if (!super.contains(file)) return false
        val orderEntry = ModuleRootManager.getInstance(module).fileIndex.getOrderEntryForFile(file)
        if (orderEntry is LibraryOrderEntry) {
            return (orderEntry.library as LibraryEx).effectiveKind(module.project) == libraryKind
        }
        return true
    }
}

fun addStdlibToJavaModuleInfo(module: Module, collector: NotificationMessageCollector): Boolean {
    if (module.sdk?.version?.isAtLeast(JavaSdkVersion.JDK_1_9) != true) return false

    val project = module.project
    val javaModule: PsiJavaModule = findFirstPsiJavaModule(module) ?: return false

    val success = WriteCommandAction.runWriteCommandAction(project, Computable<Boolean> {
        KotlinAddRequiredModuleFix.addModuleRequirement(javaModule, KOTLIN_STDLIB_MODULE_NAME)
    })

    if (!success) return false

    collector.addMessage("Added $KOTLIN_STDLIB_MODULE_NAME requirement to module-info in ${module.name}")
    return true
}
