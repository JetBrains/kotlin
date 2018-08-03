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
import org.jetbrains.kotlin.idea.core.script.dependencies.SyncScriptDependenciesLoader
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.script.findScriptDefinition
import java.io.File
import kotlin.script.experimental.dependencies.ScriptDependencies


// NOTE: this service exists exclusively because ScriptDependencyManager
// cannot be registered as implementing two services (state would be duplicated)
class IdeScriptDependenciesProvider(
    private val scriptDependenciesManager: ScriptDependenciesManager
) : ScriptDependenciesProvider {
    override fun getScriptDependencies(file: VirtualFile): ScriptDependencies? {
        return scriptDependenciesManager.getScriptDependencies(file)
    }
}

class ScriptDependenciesManager internal constructor(
    private val cacheUpdater: ScriptDependenciesUpdater,
    private val cache: ScriptDependenciesCache
) {
    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> = toVfsRoots(cacheUpdater.getCurrentDependencies(file).classpath)
    fun getScriptDependencies(file: VirtualFile): ScriptDependencies = cacheUpdater.getCurrentDependencies(file)

    fun getAllScriptsClasspathScope() = cache.allScriptsClasspathScope
    fun getAllLibrarySourcesScope() = cache.allLibrarySourcesScope
    fun getAllLibrarySources() = cache.allLibrarySources
    fun getAllScriptsClasspath() = cache.allScriptsClasspath

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ScriptDependenciesManager =
            ServiceManager.getService(project, ScriptDependenciesManager::class.java)

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

        internal val log = Logger.getInstance(ScriptDependenciesManager::class.java)

        @TestOnly
        fun updateScriptDependenciesSynchronously(virtualFile: VirtualFile, project: Project) {
            val scriptDefinition = findScriptDefinition(virtualFile, project)!!
            SyncScriptDependenciesLoader(virtualFile, scriptDefinition, project, shouldNotifyRootsChanged = true).updateDependencies()
        }
    }
}
