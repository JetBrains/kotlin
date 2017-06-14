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
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.NonClasspathDirectoriesScope
import com.intellij.util.io.URLUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.script.*
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.dependencies.KotlinScriptExternalDependencies


class IdeScriptExternalImportsProvider(
        private val scriptConfigurationManager: KotlinScriptConfigurationManager
) : KotlinScriptExternalImportsProvider {
    override fun <TF : Any> getExternalImports(file: TF): KotlinScriptExternalDependencies? {
        return scriptConfigurationManager.getExternalImports(file)
    }
}

class KotlinScriptConfigurationManager(
        private val project: Project,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider
) {

    private val cacheLock = ReentrantReadWriteLock()

    init {
        reloadScriptDefinitions()
        listenToVfsChanges()
    }

    private fun listenToVfsChanges() {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {

            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            val application = ApplicationManager.getApplication()

            override fun after(events: List<VFileEvent>) {
                if (updateExternalImportsCache(events.mapNotNull {
                    // The check is partly taken from the BuildManager.java
                    it.file?.takeIf {
                        // the isUnitTestMode check fixes ScriptConfigurationHighlighting & Navigation tests, since they are not trigger proper update mechanims
                        // TODO: find out the reason, then consider to fix tests and remove this check
                        (application.isUnitTestMode || projectFileIndex.isInContent(it)) && !ProjectUtil.isProjectOrWorkspaceFile(it)
                    }
                })) {
                    invalidateLocalCaches()
                    notifyRootsChanged()
                }
            }
        })
    }

    private val allScriptsClasspathCache = ClearableLazyValue(cacheLock) {
        val files = cache.values.flatMap { it.classpath }.distinct()
        toVfsRoots(files)
    }

    private val allScriptsClasspathScope = ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(getAllScriptsClasspath())
    }

    private val allLibrarySourcesCache = ClearableLazyValue(cacheLock) {
        toVfsRoots(cache.values.flatMap { it.sources }.distinct())
    }

    private val allLibrarySourcesScope = ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(getAllLibrarySources())
    }

    private fun notifyRootsChanged() {
        val rootsChangesRunnable = {
            runWriteAction {
                if (project.isDisposed) return@runWriteAction

                ProjectRootManagerEx.getInstanceEx(project)?.makeRootsChange(EmptyRunnable.getInstance(), false, true)
                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
            }
        }

        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode) {
            rootsChangesRunnable.invoke()
        }
        else {
            application.invokeLater(rootsChangesRunnable, ModalityState.defaultModalityState())
        }
    }

    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> = toVfsRoots(getExternalImports(file).classpath)

    fun getAllScriptsClasspath(): List<VirtualFile> = allScriptsClasspathCache.get()

    fun getAllLibrarySources(): List<VirtualFile> = allLibrarySourcesCache.get()

    fun getAllScriptsClasspathScope() = allScriptsClasspathScope.get()

    fun getAllLibrarySourcesScope() = allLibrarySourcesScope.get()

    private fun reloadScriptDefinitions() {
        val def = makeScriptDefsFromTemplatesProviderExtensions(project, { ep, ex -> log.warn("[kts] Error loading definition from ${ep.id}", ex) })
        scriptDefinitionProvider.setScriptDefinitions(def)
    }

    private val cache = hashMapOf<String, KotlinScriptExternalDependencies>()
    // TODO: this is very primitive way of synchronizing these updates
    private var lastStamp: TimeStamp = TimeStamps.next()

    fun <TF : Any> getExternalImports(file: TF): KotlinScriptExternalDependencies = cacheLock.read {
        val path = getFilePath(file)
        cache[path]?.let { return it }

        updateExternalImportsCache(listOf(file))

        return cache[path] ?: NoDependencies
    }

    private fun <TF: Any> updateExternalImportsCache(files: Iterable<TF>) = cacheLock.write {
        files.mapNotNull { file ->
            scriptDefinitionProvider.findScriptDefinition(file)?.let {
                updateForFile(file, it)
            }
        }
    }.contains(true)

    private fun <TF : Any> updateForFile(file: TF, scriptDef: KotlinScriptDefinition): Boolean {
        if (!isValidFile(file)) {
            return cache.remove(getFilePath(file)) != null
        }

        // TODO: support apis that allow for async updates for any template
        if (scriptDef is KotlinScriptDefinitionFromAnnotatedTemplate) {
            return updateAsync(file, scriptDef)
        }
        return updateSync(file, scriptDef)
    }

    fun <TF : Any> updateAsync(file: TF, scriptDefinition: KotlinScriptDefinitionFromAnnotatedTemplate): Boolean {
        val path = getFilePath(file)
        val oldDependencies = cache[path]

        val scriptContents = scriptDefinition.makeScriptContents(file, project)
        val updateStamp = TimeStamps.next()

        CompletableFuture.supplyAsync {
            val newDependencies = scriptDefinition.getDependenciesFor(oldDependencies, scriptContents)
            cacheLock.write {
                if (updateStamp >= lastStamp) {
                    lastStamp = updateStamp
                    if (cacheNewDependencies(newDependencies, oldDependencies, path)) {
                        invalidateLocalCaches()
                        notifyRootsChanged()
                    }
                }
            }
        }
        return false // not changed immediately
    }

    private fun <TF : Any> updateSync(file: TF, scriptDef: KotlinScriptDefinition): Boolean {
        val path = getFilePath(file)
        val oldDeps = cache[path]
        val deps = scriptDef.getDependenciesFor(file, project, oldDeps)
        return cacheNewDependencies(deps, oldDeps, path)
    }

    private fun cacheNewDependencies(
            new: KotlinScriptExternalDependencies?,
            old: KotlinScriptExternalDependencies?,
            path: String
    ): Boolean {
        return when {
            new != null && (old == null || !(new.match(old))) -> {
                // changed or new
                cache.put(path, new)
                true
            }
            new != null -> false // same
            cache.remove(path) != null -> true
            else -> false // unknown
        }
    }

    private fun invalidateLocalCaches() {
        allScriptsClasspathCache.clear()
        allScriptsClasspathScope.clear()
        allLibrarySourcesCache.clear()
        allLibrarySourcesScope.clear()

        val kotlinScriptDependenciesClassFinder =
                Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                        .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                        .single()

        kotlinScriptDependenciesClassFinder.clearCache()
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
                isDirectory -> StandardFileSystems.local()?.findFileByPath(this.canonicalPath)
                isFile -> StandardFileSystems.jar()?.findFileByPath(this.canonicalPath + URLUtil.JAR_SEPARATOR)
                else -> null
            }
            // TODO: report this somewhere, but do not throw: assert(res != null, { "Invalid classpath entry '$this': exists: ${exists()}, is directory: $isDirectory, is file: $isFile" })
            return res
        }

        internal val log = Logger.getInstance(KotlinScriptConfigurationManager::class.java)

        @TestOnly
        fun reloadScriptDefinitions(project: Project) {
            with(getInstance(project)) {
                reloadScriptDefinitions()
                cacheLock.write(cache::clear)
                invalidateLocalCaches()
            }
        }
    }

    private object NoDependencies: KotlinScriptExternalDependencies
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

// TODO: relying on this to compare dependencies seems wrong, doesn't take javaHome and other stuff into account
private fun KotlinScriptExternalDependencies.match(other: KotlinScriptExternalDependencies)
        = classpath.isSamePathListAs(other.classpath) &&
          sources.toSet().isSamePathListAs(other.sources.toSet()) // TODO: gradle returns stdlib and reflect sources in unstable order for some reason


private fun Iterable<File>.isSamePathListAs(other: Iterable<File>): Boolean =
        with(Pair(iterator(), other.iterator())) {
            while (first.hasNext() && second.hasNext()) {
                if (first.next().canonicalPath != second.next().canonicalPath) return false
            }
            !(first.hasNext() || second.hasNext())
        }

data class TimeStamp(private val stamp: Long) {
    operator fun compareTo(other: TimeStamp) = this.stamp.compareTo(other.stamp)
}

object TimeStamps {
    private var current: Long = 0

    fun next() = TimeStamp(current++)
}
