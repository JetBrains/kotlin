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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.KotlinScriptExternalImportsProvider
import org.jetbrains.kotlin.script.makeScriptDefsFromTemplatesProviderExtensions
import java.io.File
import kotlin.script.dependencies.ScriptDependencies


// NOTE: this service exists exclusively because KotlinScriptConfigurationManager
// cannot be registered as implementing two services (state would be duplicated)
class IdeScriptExternalImportsProvider(
        private val scriptConfigurationManager: KotlinScriptConfigurationManager
) : KotlinScriptExternalImportsProvider {
    override fun getScriptDependencies(file: VirtualFile): ScriptDependencies? {
        return scriptConfigurationManager.getScriptDependencies(file)
    }
}

class KotlinScriptConfigurationManager(
        private val project: Project,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider
) {
    private val cache = DependenciesCache(project)
    private val cacheUpdater = ScriptDependenciesUpdater(project, cache, scriptDefinitionProvider)

    init {
        reloadScriptDefinitions()
    }

    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> = toVfsRoots(cacheUpdater.getCurrentDependencies(file).classpath)
    fun getScriptDependencies(file: VirtualFile) = cacheUpdater.getCurrentDependencies(file)

    private fun reloadScriptDefinitions() {
        val def = makeScriptDefsFromTemplatesProviderExtensions(project, { ep, ex -> log.warn("[kts] Error loading definition from ${ep.id}", ex) })
        scriptDefinitionProvider.setScriptDefinitions(def)
    }

    fun getAllScriptsClasspathScope() = cache.allScriptsClasspathScope.get()
    fun getAllLibrarySourcesScope() = cache.allLibrarySourcesScope.get()
    fun getAllLibrarySources() = cache.allLibrarySourcesCache.get()
    fun getAllScriptsClasspath() = cache.allScriptsClasspathCache.get()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): KotlinScriptConfigurationManager =
                ServiceManager.getService(project, KotlinScriptConfigurationManager::class.java)

        fun toVfsRoots(roots: Iterable<File>): List<VirtualFile> {
            return roots.mapNotNull { it.classpathEntryToVfs() }
        }

        private fun File.classpathEntryToVfs(): VirtualFile? {
            val res = when {
                !exists() -> null
                isDirectory -> StandardFileSystems.local()?.findFileByPath(this.canonicalPath)
                isFile -> StandardFileSystems.jar()?.findFileByPath(this.canonicalPath + URLUtil.JAR_SEPARATOR)
                else -> null
            }
            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })
            return res
        }

        internal val log = Logger.getInstance(KotlinScriptConfigurationManager::class.java)

        @TestOnly
        fun updateScriptDependenciesSynchronously(virtualFile: VirtualFile, project: Project) {
            with(getInstance(project)) {
                val scriptDefinition = KotlinScriptDefinitionProvider.getInstance(project)!!.findScriptDefinition(virtualFile)!!
                val updated = cacheUpdater.updateSync(virtualFile, scriptDefinition)
                assert(updated)
                cacheUpdater.onChange()
            }
        }

        @TestOnly
        fun reloadScriptDefinitions(project: Project) {
            with(getInstance(project)) {
                reloadScriptDefinitions()
                cacheUpdater.clear()
            }
        }
    }
}
