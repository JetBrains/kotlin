/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

internal abstract class ScriptClassRootsCache(private val project: Project) {
    protected abstract val all: Collection<Pair<VirtualFile, ScriptCompilationConfigurationWrapper>>
    protected abstract fun getConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper?

    private fun getScriptSdk(compilationConfiguration: ScriptCompilationConfigurationWrapper?): Sdk? {
        // workaround for mismatched gradle wrapper and plugin version
        val javaHome = try {
            compilationConfiguration?.javaHome?.let { VfsUtil.findFileByIoFile(it, true) }
        } catch (e: Throwable) {
            null
        } ?: return null

        return getAllProjectSdks().find { it.homeDirectory == javaHome }
    }

    private val scriptsSdksCache: Map<VirtualFile, Sdk?> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = getConfiguration(file)
            return@createWeakMap getScriptSdk(compilationConfiguration)
                ?: ScriptConfigurationManager.getScriptDefaultSdk(project)
        }

    fun getScriptSdk(file: VirtualFile): Sdk? = scriptsSdksCache[file]

    val firstScriptSdk: Sdk? by lazy {
        val firstCachedScript = all.firstOrNull() ?: return@lazy null
        return@lazy getScriptSdk(firstCachedScript.second)
    }

    private val allSdks by lazy {
        all.mapNotNull { scriptsSdksCache[it.first] }
            .distinct()
    }

    private val allNonIndexedSdks by lazy {
        all.mapNotNull { scriptsSdksCache[it.first] }
            .filterNonModuleSdk()
            .distinct()
    }

    private fun List<Sdk>.filterNonModuleSdk(): List<Sdk> {
        val moduleSdks = ModuleManager.getInstance(project).modules.map { ModuleRootManager.getInstance(it).sdk }
        return filterNot { moduleSdks.contains(it) }
    }

    val allDependenciesClassFiles by lazy {
        val sdkFiles = allNonIndexedSdks.flatMap { it.rootProvider.getFiles(OrderRootType.CLASSES).toList() }
        val scriptDependenciesClasspath = all.flatMap { it.second.dependenciesClassPath }.distinct()

        sdkFiles + ScriptConfigurationManager.toVfsRoots(scriptDependenciesClasspath)
    }

    val allDependenciesSources by lazy {
        val sdkSources = allNonIndexedSdks.flatMap { it.rootProvider.getFiles(OrderRootType.SOURCES).toList() }
        val scriptDependenciesSources = all.flatMap { it.second.dependenciesSources }.distinct()

        sdkSources + ScriptConfigurationManager.toVfsRoots(scriptDependenciesSources)
    }

    val allDependenciesClassFilesScope by lazy {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    val allDependenciesSourcesScope by lazy {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    private val scriptsDependenciesClasspathScopeCache: MutableMap<VirtualFile, GlobalSearchScope> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val compilationConfiguration = getConfiguration(file)
                ?: return@createWeakMap GlobalSearchScope.EMPTY_SCOPE

            val roots = compilationConfiguration.dependenciesClassPath
            val sdk = scriptsSdksCache[file]

            @Suppress("FoldInitializerAndIfToElvis")
            if (sdk == null) {
                return@createWeakMap NonClasspathDirectoriesScope.compose(
                    ScriptConfigurationManager.toVfsRoots(
                        roots
                    )
                )
            }

            return@createWeakMap NonClasspathDirectoriesScope.compose(
                sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() + ScriptConfigurationManager.toVfsRoots(
                    roots
                )
            )
        }

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope {
        return scriptsDependenciesClasspathScopeCache[file] ?: GlobalSearchScope.EMPTY_SCOPE
    }

    fun hasNotCachedRoots(compilationConfiguration: ScriptCompilationConfigurationWrapper): Boolean {
        val scriptSdk = getScriptSdk(compilationConfiguration)
            ?: ScriptConfigurationManager.getScriptDefaultSdk(project)

        val wasSdkChanged = scriptSdk != null && !allSdks.contains(scriptSdk)
        if (wasSdkChanged) {
            debug { "sdk was changed: $compilationConfiguration" }
            return true
        }

        val newClassRoots = ScriptConfigurationManager.toVfsRoots(compilationConfiguration.dependenciesClassPath)
        for (newClassRoot in newClassRoots) {
            if (!allDependenciesClassFiles.contains(newClassRoot)) {
                debug { "class root was changed: $newClassRoot" }
                return true
            }
        }

        val newSourceRoots = ScriptConfigurationManager.toVfsRoots(compilationConfiguration.dependenciesSources)
        for (newSourceRoot in newSourceRoots) {
            if (!allDependenciesSources.contains(newSourceRoot)) {
                debug { "source root was changed: $newSourceRoot" }
                return true
            }
        }

        return false
    }
}