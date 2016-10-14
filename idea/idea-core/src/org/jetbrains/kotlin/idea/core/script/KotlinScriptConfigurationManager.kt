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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.KotlinScriptExternalImportsProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.script.makeScriptDefsFromTemplatesProviderExtensions
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Suppress("SimplifyAssertNotNull")
class KotlinScriptConfigurationManager(
        private val project: Project,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider,
        private val scriptExternalImportsProvider: KotlinScriptExternalImportsProvider
) {

    init {
        reloadScriptDefinitions()

        StartupManager.getInstance(project).registerPostStartupActivity {
            cacheAllScriptsExtraImports()
            invalidateLocalCaches()
            notifyRootsChanged()
        }

        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {
            override fun after(events: List<VFileEvent>) {
                updateExternalImportsCache(events.mapNotNull { it.file }) {
                    invalidateLocalCaches()
                    notifyRootsChanged()
                }
            }
        })
    }

    private val cacheLock = ReentrantReadWriteLock()

    private val allScriptsClasspathCache = ClearableLazyValue(cacheLock) {
        toVfsRoots(scriptExternalImportsProvider.getKnownCombinedClasspath().distinct())
    }

    private val allLibrarySourcesCache = ClearableLazyValue(cacheLock) {
        toVfsRoots(scriptExternalImportsProvider.getKnownSourceRoots().distinct())
    }

    private fun notifyRootsChanged() {
        // TODO: it seems invokeLater leads to inconsistent behaviour (at least in tests)
        ApplicationManager.getApplication().invokeLater {
            runWriteAction { ProjectRootManagerEx.getInstanceEx(project)?.makeRootsChange(EmptyRunnable.getInstance(), false, true) }
        }
    }

    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> = toVfsRoots(
            scriptExternalImportsProvider.getExternalImports(file)?.classpath ?: emptyList()
    )

    fun getAllScriptsClasspath(): List<VirtualFile> = allScriptsClasspathCache.get()

    fun getAllLibrarySources(): List<VirtualFile> = allLibrarySourcesCache.get()

    fun getAllScriptsClasspathScope() = NonClasspathDirectoriesScope(getAllScriptsClasspath())

    fun getAllLibrarySourcesScope() = NonClasspathDirectoriesScope(getAllLibrarySources())

    private fun reloadScriptDefinitions() {
        makeScriptDefsFromTemplatesProviderExtensions(project, { ep, ex -> log.warn("[kts] Error loading definition from ${ep.id}", ex) }).let {
            if (it.isNotEmpty()) {
                scriptDefinitionProvider.setScriptDefinitions(it + StandardScriptDefinition)
            }
        }
    }

    private fun cacheAllScriptsExtraImports() {
        runReadAction {
            scriptExternalImportsProvider.apply {
                invalidateCaches()
                cacheExternalImports(
                        scriptDefinitionProvider.getAllKnownFileTypes()
                                .flatMap { FileTypeIndex.getFiles(it, GlobalSearchScope.allScope(project)) })
            }
        }
    }

    private fun updateExternalImportsCache(files: Iterable<VirtualFile>, onChange: () -> Unit) {
        val isChanged = scriptExternalImportsProvider.updateExternalImportsCache(files).any()
        if (isChanged) {
            onChange()
        }
    }

    private fun invalidateLocalCaches() {
        allScriptsClasspathCache.clear()
        allLibrarySourcesCache.clear()
    }


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
                isDirectory -> StandardFileSystems.local()?.findFileByPath(this.canonicalPath) ?: null
                isFile -> StandardFileSystems.jar()?.findFileByPath(this.canonicalPath + URLUtil.JAR_SEPARATOR) ?: null
                else -> null
            }
            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })
            return res
        }
        internal val log = Logger.getInstance(KotlinScriptConfigurationManager::class.java)

        @TestOnly
        fun reloadScriptDefinitions(project: Project) {
            with (getInstance(project)) {
                reloadScriptDefinitions()
                scriptExternalImportsProvider.invalidateCaches()
                invalidateLocalCaches()
            }
        }
    }
}

private class ClearableLazyValue<out T : Any>(private val lock: ReentrantReadWriteLock, private val compute: () -> T) {
    private var value: T? = null

    fun get(): T {
        lock.read {
            if (value == null) {
                lock.write {
                    value = compute()
                }
            }
            return value!!
        }
    }

    fun clear() {
        lock.write {
            value = null
        }
    }
}