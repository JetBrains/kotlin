/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.LOG
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File

class ScriptClassRootsCache(
    val scripts: Map<String, LightScriptInfo>,
    val classes: Set<String>,
    val sources: Set<String>,
    val sdks: Map<String, Sdk?>
) {
    abstract class LightScriptInfo {
        abstract fun buildConfiguration(): ScriptCompilationConfigurationWrapper?
    }

    private class HeavyScriptInfo(
        val scriptConfiguration: ScriptCompilationConfigurationWrapper,
        val classFilesScope: GlobalSearchScope,
        val sdk: Sdk?
    )

    class DirectScriptInfo(val result: ScriptCompilationConfigurationWrapper) : LightScriptInfo() {
        override fun buildConfiguration(): ScriptCompilationConfigurationWrapper = result
    }

    class Builder(val project: Project) {
        private val defaultSdk = getScriptDefaultSdk()

        val scripts = mutableMapOf<String, LightScriptInfo>()
        val sdks = mutableMapOf<String, Sdk?>()

        val classes = mutableSetOf<String>()
        val sources = mutableSetOf<String>()

        fun add(
            vFile: VirtualFile,
            configuration: ScriptCompilationConfigurationWrapper
        ) {
            addSdk(configuration.javaHome)

            configuration.dependenciesClassPath.forEach { classes.add(it.absolutePath) }
            configuration.dependenciesSources.forEach { sources.add(it.absolutePath) }

            scripts[vFile.path] = DirectScriptInfo(configuration)
        }

        fun build() = ScriptClassRootsCache(scripts, classes, sources, sdks)

        fun addSdk(javaHome: File?): Sdk? {
            if (javaHome == null) return defaultSdk
            val canonicalPath = javaHome.canonicalPath
            return sdks.getOrPut(canonicalPath) {
                getScriptSdkByJavaHome(javaHome) ?: defaultSdk
            }
        }

        private fun getScriptSdkByJavaHome(javaHome: File): Sdk? {
            // workaround for mismatched gradle wrapper and plugin version
            val javaHomeVF = try {
                VfsUtil.findFileByIoFile(javaHome, true)
            } catch (e: Throwable) {
                null
            } ?: return null

            return getProjectJdkTableSafe().allJdks.find { it.homeDirectory == javaHomeVF }
        }

        private fun getScriptDefaultSdk(): Sdk? {
            val projectSdk = ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.canBeUsedForScript() }
            if (projectSdk != null) return projectSdk

            val anyJavaSdk = getAllProjectSdks().find { it.canBeUsedForScript() }
            if (anyJavaSdk != null) {
                return anyJavaSdk
            }

            LOG.warn(
                "Default Script SDK is null: " +
                        "projectSdk = ${ProjectRootManager.getInstance(project).projectSdk}, " +
                        "all sdks = ${getAllProjectSdks().joinToString("\n")}"
            )

            return null
        }

        private fun Sdk.canBeUsedForScript() = sdkType is JavaSdkType
    }

    fun getLightScriptInfo(file: String) = scripts[file]

    fun contains(file: VirtualFile): Boolean = file.path in scripts

    val firstScriptSdk: Sdk? = sdks.values.firstOrNull()

    val allDependenciesClassFiles: List<VirtualFile> by lazy {
        classes.mapNotNull { StandardFileSystems.local().findFileByPath(it) }
    }

    val allDependenciesSources: List<VirtualFile> by lazy {
        sources.mapNotNull { StandardFileSystems.local().findFileByPath(it) }
    }

    val allDependenciesClassFilesScope by lazy {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    val allDependenciesSourcesScope by lazy {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    private val scriptsDependenciesCache: MutableMap<VirtualFile, HeavyScriptInfo> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val configuration = scripts[file.path]?.buildConfiguration() ?: return@createWeakMap null

            val roots = configuration.dependenciesClassPath
            val sdk = sdks[configuration.javaHome?.canonicalPath]

            @Suppress("FoldInitializerAndIfToElvis")
            if (sdk == null) {
                return@createWeakMap HeavyScriptInfo(
                    configuration,
                    NonClasspathDirectoriesScope.compose(ScriptConfigurationManager.toVfsRoots(roots)),
                    null
                )
            }

            return@createWeakMap HeavyScriptInfo(
                configuration,
                NonClasspathDirectoriesScope.compose(
                    sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() + ScriptConfigurationManager.toVfsRoots(roots)
                ),
                sdk
            )
        }

    fun getScriptConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? =
        scriptsDependenciesCache[file]?.scriptConfiguration

    fun getScriptSdk(file: VirtualFile): Sdk? =
        scriptsDependenciesCache[file]?.sdk

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        scriptsDependenciesCache[file]?.classFilesScope ?: GlobalSearchScope.EMPTY_SCOPE
}

