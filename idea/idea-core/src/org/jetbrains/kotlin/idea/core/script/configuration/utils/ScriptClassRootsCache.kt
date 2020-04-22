/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.File

abstract class ScriptClassRootsCache(
    private val project: Project,
    private val rootsCacheKey: ScriptClassRootsStorage.Companion.Key,
    private val roots: ScriptClassRootsStorage.Companion.ScriptClassRoots
) {
    protected abstract fun getConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper?

    abstract val firstScriptSdk: Sdk?

    abstract fun getScriptSdk(file: VirtualFile): Sdk?

    abstract fun contains(file: VirtualFile): Boolean

    private class Fat(
        val scriptConfiguration: ScriptCompilationConfigurationWrapper,
        val classFilesScope: GlobalSearchScope
    )

    val allDependenciesClassFiles by lazy {
        ScriptClassRootsStorage.getInstance(project).loadClasspathRoots(rootsCacheKey)
    }

    val allDependenciesSources by lazy {
        ScriptClassRootsStorage.getInstance(project).loadSourcesRoots(rootsCacheKey)
    }

    val allDependenciesClassFilesScope by lazy {
        NonClasspathDirectoriesScope.compose(allDependenciesClassFiles)
    }

    val allDependenciesSourcesScope by lazy {
        NonClasspathDirectoriesScope.compose(allDependenciesSources)
    }

    private val scriptsDependenciesCache: MutableMap<VirtualFile, Fat> =
        ConcurrentFactoryMap.createWeakMap { file ->
            val configuration = getConfiguration(file) ?: return@createWeakMap null

            val roots = configuration.dependenciesClassPath
            val sdk = getScriptSdk(file)

            @Suppress("FoldInitializerAndIfToElvis")
            if (sdk == null) {
                return@createWeakMap Fat(
                    configuration,
                    NonClasspathDirectoriesScope.compose(ScriptConfigurationManager.toVfsRoots(roots))
                )
            }

            return@createWeakMap Fat(
                configuration,
                NonClasspathDirectoriesScope.compose(
                    sdk.rootProvider.getFiles(OrderRootType.CLASSES).toList() + ScriptConfigurationManager.toVfsRoots(roots)
                )
            )
        }

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope {
        return scriptsDependenciesCache[file]?.classFilesScope ?: GlobalSearchScope.EMPTY_SCOPE
    }

    fun getScriptConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? {
        return scriptsDependenciesCache[file]?.scriptConfiguration
    }

    fun hasNotCachedRoots(roots: ScriptClassRootsStorage.Companion.ScriptClassRoots): Boolean {
        return !ScriptClassRootsStorage.getInstance(project).containsAll(rootsCacheKey, roots)
    }

    fun saveClassRootsToStorage() {
        val rootsStorage = ScriptClassRootsStorage.getInstance(project)
        if (roots.classpathFiles.isNotEmpty() || roots.sourcesFiles.isNotEmpty() || roots.sdks.isNotEmpty()) {
            rootsStorage.save(rootsCacheKey, roots)
        }
    }

    companion object {
        fun toStringValues(prop: Collection<File>): Set<String> {
            return prop.mapNotNull { it.absolutePath }.toSet()
        }

        fun getScriptSdkOfDefault(javaHomeStr: File?, project: Project): Sdk? {
            return getScriptSdk(javaHomeStr) ?: ScriptConfigurationManager.getScriptDefaultSdk(project)
        }

        fun getScriptSdk(javaHomeStr: File?): Sdk? {
            // workaround for mismatched gradle wrapper and plugin version
            val javaHome = try {
                javaHomeStr?.let { VfsUtil.findFileByIoFile(it, true) }
            } catch (e: Throwable) {
                null
            } ?: return null

            return getProjectJdkTableSafe().allJdks.find { it.homeDirectory == javaHome }
        }

        fun Sdk.isAlreadyIndexed(project: Project): Boolean {
            return ModuleManager.getInstance(project).modules.any { ModuleRootManager.getInstance(it).sdk == this }
        }

        fun empty(project: Project) = object : ScriptClassRootsCache(
            project,
            ScriptClassRootsStorage.Companion.Key("empty"),
            ScriptClassRootsStorage.Companion.ScriptClassRoots(
                setOf(),
                setOf(),
                setOf()
            )
        ) {
            override fun getConfiguration(file: VirtualFile): ScriptCompilationConfigurationWrapper? = null

            override val firstScriptSdk: Sdk? = null

            override fun getScriptSdk(file: VirtualFile): Sdk? = null

            override fun contains(file: VirtualFile): Boolean = true
        }
    }
}

