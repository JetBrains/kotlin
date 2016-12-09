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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import org.jetbrains.kotlin.idea.core.script.KotlinScriptConfigurationManager
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.KotlinScriptExternalImportsProvider
import org.jetbrains.kotlin.utils.singletonOrEmptyList

class ScriptModuleSearchScope(val scriptFile: VirtualFile, baseScope: GlobalSearchScope) : DelegatingGlobalSearchScope(baseScope) {
    override fun equals(other: Any?) = other is ScriptModuleSearchScope && scriptFile == other.scriptFile && super.equals(other)

    override fun hashCode() = scriptFile.hashCode() * 73 * super.hashCode()
}

data class ScriptModuleInfo(val project: Project, val scriptFile: VirtualFile,
                            val scriptDefinition: KotlinScriptDefinition) : IdeaModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.OTHER

    val externalDependencies by lazy {
        KotlinScriptExternalImportsProvider.getInstance(project)?.getExternalImports(scriptFile)
    }

    override val name: Name = Name.special("<script ${scriptFile.name} ${scriptDefinition.name}>")

    override fun contentScope() = GlobalSearchScope.fileScope(project, scriptFile)

    override fun dependencies(): List<IdeaModuleInfo> {
        return listOf(
                this,
                ScriptDependenciesModuleInfo(project, externalDependencies, this)
        ) + sdkDependencies(externalDependencies, project)
    }
}

private fun sdkDependencies(scriptDependencies: KotlinScriptExternalDependencies?, project: Project): List<SdkInfo>
        = findJdk(scriptDependencies, project)?.let { SdkInfo(project, it) }.singletonOrEmptyList()

fun findJdk(dependencies: KotlinScriptExternalDependencies?, project: Project): Sdk? {
    val allJdks = getAllProjectSdks()
    // workaround for mismatched gradle wrapper and plugin version
    val javaHome = try { dependencies?.javaHome } catch (e: Throwable) { null }

    return allJdks.find { javaHome != null && it.homePath == javaHome } ?:
           ProjectRootManager.getInstance(project).projectSdk ?:
           allJdks.firstOrNull()
}

class ScriptDependenciesModuleInfo(
        val project: Project,
        val dependencies: KotlinScriptExternalDependencies?,
        val scriptModuleInfo: ScriptModuleInfo?
): IdeaModuleInfo {
    override fun dependencies() = (listOf(this) + sdkDependencies(dependencies, project))

    override val name = Name.special("<Script dependencies>")

    override fun contentScope(): GlobalSearchScope {
        if (dependencies == null) {
            // we do not know which scripts these dependencies are
            return KotlinSourceFilterScope.libraryClassFiles(
                    KotlinScriptConfigurationManager.getInstance(project).getAllScriptsClasspathScope(), project
            )
        }
        val classpath = KotlinScriptConfigurationManager.toVfsRoots(dependencies.classpath)
        // TODO: this is not very efficient because KotlinSourceFilterScope already checks if the files are in scripts classpath
        return KotlinSourceFilterScope.libraryClassFiles(NonClasspathDirectoriesScope(classpath), project)
    }

    // NOTE: intentionally not taking dependencies into account
    // otherwise there is no way to implement getModuleInfo
    override fun hashCode() = project.hashCode()
    override fun equals(other: Any?): Boolean = other is ScriptDependenciesModuleInfo && this.project == other.project

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY
}