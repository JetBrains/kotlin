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

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.IndexableSetContributor
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.idea.caches.resolve.FileLibraryScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class KotlinScriptConfigurationManager(
        private val project: Project,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider,
        private val scriptExternalImportsProvider: KotlinScriptExternalImportsProvider?
) {

    private val kotlinEnvVars: Map<String, List<String>> by lazy {
        generateKotlinScriptClasspathEnvVarsFromPaths(project, PathUtil.getKotlinPathsForIdeaPlugin())
    }

    init {
        reloadScriptDefinitions()
        // TODO: sort out read/write action business and if possible make it lazy (e.g. move to getAllScriptsClasspath)
        runReadAction { cacheAllScriptsExtraImports() }
    }

    private var allScriptsClasspathCache: List<VirtualFile>? = null
    private val cacheLock = ReentrantReadWriteLock()

    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> =
            scriptExternalImportsProvider
                    ?.getExternalImports(file)
                    ?.flatMap { it.classpath }
                    ?.map { it.classpathEntryToVfs() }
            ?: emptyList()

    fun getAllScriptsClasspath(): List<VirtualFile> = cacheLock.read {
        if (allScriptsClasspathCache == null) {
            allScriptsClasspathCache =
                    (scriptExternalImportsProvider?.getKnownCombinedClasspath() ?: emptyList())
                    .distinct()
                    .mapNotNull { it.classpathEntryToVfs() }
        }
        return allScriptsClasspathCache!!
    }

    private fun File.classpathEntryToVfs(): VirtualFile =
            if (isDirectory)
                StandardFileSystems.local()?.findFileByPath(this.canonicalPath) ?: throw FileNotFoundException("Classpath entry points to a non-existent location: ${this}")
            else
                StandardFileSystems.jar()?.findFileByPath(this.canonicalPath + URLUtil.JAR_SEPARATOR) ?: throw FileNotFoundException("Classpath entry points to a file that is not a JAR archive: ${this}")

    fun getAllScriptsClasspathScope(): GlobalSearchScope? {
        return getAllScriptsClasspath().let { cp ->
            if (cp.isEmpty()) null
            else GlobalSearchScope.union(cp.map { FileLibraryScope(project, it) }.toTypedArray())
        }
    }

    private fun reloadScriptDefinitions() {
        (makeScriptDefsFromTemplateProviderExtensions(project /* TODO: add logging here */) +
         loadScriptConfigsFromProjectRoot(File(project.basePath ?: "")).map { KotlinConfigurableScriptDefinition(it, kotlinEnvVars) } +
         makeScriptDefsFromConfigs(loadScriptDefConfigsFromProjectRoot(File(project.basePath ?: "")))).let {
            if (it.isNotEmpty()) {
                scriptDefinitionProvider.setScriptDefinitions(it + StandardScriptDefinition)
            }
        }
    }

    private fun cacheAllScriptsExtraImports() {
        scriptExternalImportsProvider?.apply {
            invalidateCaches()
            cacheExternalImports(
                scriptDefinitionProvider.getAllKnownFileTypes()
                        .flatMap { FileTypeIndex.getFiles(it, GlobalSearchScope.allScope(project)) })
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinScriptConfigurationManager =
                ServiceManager.getService(project, KotlinScriptConfigurationManager::class.java)
    }
}


class KotlinScriptDependenciesIndexableSetContributor : IndexableSetContributor() {

    override fun getAdditionalProjectRootsToIndex(project: Project): Set<VirtualFile> =
            KotlinScriptConfigurationManager.getInstance(project).getAllScriptsClasspath().toSet()

    override fun getAdditionalRootsToIndex(): Set<VirtualFile> = emptySet()
}

