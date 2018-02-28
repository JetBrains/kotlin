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
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.script.KotlinScriptDefinition
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

    val externalDependencies: ScriptDependencies
        get() = ScriptDependenciesManager.getInstance(project).getScriptDependencies(scriptFile)

    override val name: Name = Name.special("<script ${scriptFile.name} ${scriptDefinition.name}>")

    override fun contentScope() = GlobalSearchScope.fileScope(project, scriptFile)

    override fun dependencies(): List<IdeaModuleInfo> {
        return listOf(
            this, ScriptDependenciesModuleInfo(project, this)
        ) + sdkDependencies(externalDependencies, project)
    }
}

private fun sdkDependencies(scriptDependencies: ScriptDependencies?, project: Project): List<SdkInfo> =
    listOfNotNull(findJdk(scriptDependencies, project)?.let { SdkInfo(project, it) })

fun findJdk(dependencies: ScriptDependencies?, project: Project): Sdk? {
    val allJdks = getAllProjectSdks()
    // workaround for mismatched gradle wrapper and plugin version
    val javaHome = try {
        dependencies?.javaHome?.canonicalPath
    } catch (e: Throwable) {
        null
    }

    return allJdks.find { javaHome != null && it.homePath == javaHome } ?: ProjectRootManager.getInstance(project).projectSdk
    ?: allJdks.firstOrNull()
}

class ScriptDependenciesModuleInfo(
    val project: Project,
    val scriptModuleInfo: ScriptModuleInfo?
) : IdeaModuleInfo, BinaryModuleInfo {
    override fun dependencies() = (listOf(this) + sdkDependencies(
        scriptModuleInfo?.externalDependencies,
        project
    ))

    override val name = Name.special("<Script dependencies>")

    override fun contentScope(): GlobalSearchScope {
        if (scriptModuleInfo == null) {
            // we do not know which scripts these dependencies are
            return KotlinSourceFilterScope.libraryClassFiles(
                ScriptDependenciesManager.getInstance(project).getAllScriptsClasspathScope(), project
            )
        }
        return ServiceManager.getService(project, ScriptBinariesScopeCache::class.java).get(scriptModuleInfo.externalDependencies)
    }

    // NOTE: intentionally not taking corresponding script info into account
    // otherwise there is no way to implement getModuleInfo
    override fun hashCode() = project.hashCode()

    override fun equals(other: Any?): Boolean = other is ScriptDependenciesModuleInfo && this.project == other.project

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY

    override val sourcesModuleInfo: SourceForBinaryModuleInfo?
        get() = ScriptDependenciesSourceModuleInfo(project)
}

data class ScriptDependenciesSourceModuleInfo(
    val project: Project
) : IdeaModuleInfo, SourceForBinaryModuleInfo {
    override val name = Name.special("<Source for script dependencies>")

    override val binariesModuleInfo: ScriptDependenciesModuleInfo
        get() = ScriptDependenciesModuleInfo(project, null)

    override fun sourceScope(): GlobalSearchScope = KotlinSourceFilterScope.librarySources(
        ScriptDependenciesManager.getInstance(project).getAllLibrarySourcesScope(), project
    )

}

private class ScriptBinariesScopeCache(private val project: Project) : SLRUCache<ScriptDependencies, GlobalSearchScope>(6, 6) {
    override fun createValue(key: ScriptDependencies?): GlobalSearchScope {
        val roots = key?.classpath ?: emptyList()
        val classpath = ScriptDependenciesManager.toVfsRoots(roots)
        // TODO: this is not very efficient because KotlinSourceFilterScope already checks if the files are in scripts classpath
        return KotlinSourceFilterScope.libraryClassFiles(NonClasspathDirectoriesScope(classpath), project)
    }
}