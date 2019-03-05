/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesCache.Companion.MAX_SCRIPTS_CACHED
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.dependencies.ScriptRelatedModulesProvider
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.PlatformDependentCompilerServices
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.File
import kotlin.script.experimental.dependencies.ScriptDependencies

class ScriptModuleSearchScope(val scriptFile: VirtualFile, baseScope: GlobalSearchScope) : DelegatingGlobalSearchScope(baseScope) {
    override fun equals(other: Any?) = other is ScriptModuleSearchScope && scriptFile == other.scriptFile && super.equals(other)

    override fun hashCode() = scriptFile.hashCode() * 73 * super.hashCode()
}

data class ScriptModuleInfo(
    val project: Project,
    val scriptFile: VirtualFile,
    val scriptDefinition: KotlinScriptDefinition
) : IdeaModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.OTHER

    override val name: Name = Name.special("<script ${scriptFile.name} ${scriptDefinition.name}>")

    override fun contentScope() = GlobalSearchScope.fileScope(project, scriptFile)

    override fun dependencies(): List<IdeaModuleInfo> {
        return arrayListOf<IdeaModuleInfo>(this).apply {
            val scriptDependentModules = ScriptRelatedModulesProvider.getRelatedModules(scriptFile, project)
            if (scriptDependentModules.isNotEmpty()) {
                scriptDependentModules.mapNotNull { it.productionSourceInfo() ?: it.testSourceInfo() }.forEach {
                    this@apply.add(it)
                    this@apply.addAll(it.dependencies())
                }
            }

            val dependenciesInfo = ScriptDependenciesInfo.ForFile(project, scriptFile, scriptDefinition)
            add(dependenciesInfo)

            dependenciesInfo.sdk?.let { add(SdkInfo(project, it)) }
        }
    }

    override val platform: TargetPlatform?
        get() = null

    override val compilerServices: PlatformDependentCompilerServices?
        get() = null
}

fun findJdk(dependencies: ScriptDependencies?, project: Project): Sdk? {
    val allJdks = getAllProjectSdks()
    // workaround for mismatched gradle wrapper and plugin version
    val javaHome = try {
        dependencies?.javaHome?.canonicalPath
    } catch (e: Throwable) {
        null
    }

    return allJdks.find { javaHome != null && File(it.homePath).canonicalPath == javaHome }
            ?: ProjectRootManager.getInstance(project).projectSdk
            ?: allJdks.firstOrNull()
}

sealed class ScriptDependenciesInfo(val project: Project) : IdeaModuleInfo, BinaryModuleInfo {
    abstract val sdk: Sdk?

    override val name = Name.special("<Script dependencies>")

    override fun dependencies(): List<IdeaModuleInfo> = listOfNotNull(this, sdk?.let { SdkInfo(project, it) })

    // NOTE: intentionally not taking corresponding script info into account
    // otherwise there is no way to implement getModuleInfo
    override fun hashCode() = project.hashCode()

    override fun equals(other: Any?): Boolean = other is ScriptDependenciesInfo && this.project == other.project

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY

    override val sourcesModuleInfo: SourceForBinaryModuleInfo?
        get() = ScriptDependenciesSourceInfo.ForProject(project)

    override val platform: TargetPlatform?
        get() = null

    override val compilerServices: PlatformDependentCompilerServices?
        get() = null

    class ForFile(
        project: Project,
        val scriptFile: VirtualFile,
        val scriptDefinition: KotlinScriptDefinition
    ) : ScriptDependenciesInfo(project) {
        private val externalDependencies: ScriptDependencies
            get() = ScriptDependenciesManager.getInstance(project).getScriptDependencies(scriptFile)

        override val sdk: Sdk?
            get() = findJdk(externalDependencies, project)

        override fun contentScope(): GlobalSearchScope {
            return ServiceManager.getService(project, ScriptBinariesScopeCache::class.java).get(externalDependencies)
        }
    }

    class ForProject(project: Project) : ScriptDependenciesInfo(project) {
        override val sdk: Sdk?
            get() = findJdk(null, project)

        override fun contentScope(): GlobalSearchScope {
            // we do not know which scripts these dependencies are
            return KotlinSourceFilterScope.libraryClassFiles(
                ScriptDependenciesManager.getInstance(project).getAllScriptsClasspathScope(), project
            )
        }
    }
}

sealed class ScriptDependenciesSourceInfo(val project: Project) : IdeaModuleInfo, SourceForBinaryModuleInfo {
    override val name = Name.special("<Source for script dependencies>")

    override val binariesModuleInfo: ScriptDependenciesInfo
        get() = ScriptDependenciesInfo.ForProject(project)

    override fun sourceScope(): GlobalSearchScope = KotlinSourceFilterScope.librarySources(
        ScriptDependenciesManager.getInstance(project).getAllLibrarySourcesScope(), project
    )

    override fun hashCode() = project.hashCode()

    override fun equals(other: Any?): Boolean = other is ScriptDependenciesSourceInfo && this.project == other.project

    override val platform: TargetPlatform?
        get() = null

    override val compilerServices: PlatformDependentCompilerServices?
        get() = null

    class ForProject(project: Project) : ScriptDependenciesSourceInfo(project)
}

private class ScriptBinariesScopeCache(private val project: Project) : SLRUCache<ScriptDependencies, GlobalSearchScope>(MAX_SCRIPTS_CACHED, MAX_SCRIPTS_CACHED) {
    override fun createValue(key: ScriptDependencies?): GlobalSearchScope {
        val roots = key?.classpath ?: emptyList()
        val classpath = ScriptDependenciesManager.toVfsRoots(roots)
        // TODO: this is not very efficient because KotlinSourceFilterScope already checks if the files are in scripts classpath
        return KotlinSourceFilterScope.libraryClassFiles(NonClasspathDirectoriesScope(classpath), project)
    }
}