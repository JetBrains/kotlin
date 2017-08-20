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
import java.lang.Math.max
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Supplier
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.dependencies.KotlinScriptExternalDependencies


// NOTE: this service exists exclusively because KotlinScriptConfigurationManager
// cannot be registered as implementing two services (state would be duplicated)
class IdeScriptExternalImportsProvider(
        private val scriptConfigurationManager: KotlinScriptConfigurationManager
) : KotlinScriptExternalImportsProvider {
    override fun getScriptDependencies(file: VirtualFile): KotlinScriptExternalDependencies? {
        return scriptConfigurationManager.getScriptDependencies(file)
    }
}

class KotlinScriptConfigurationManager(
        private val project: Project,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider
) : KotlinScriptExternalImportsProviderBase(project) {
    private val cacheLock = ReentrantReadWriteLock()
    private val threadPool = newFixedThreadPool(max(1, Runtime.getRuntime().availableProcessors() / 2))

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
        val files = cache.values.flatMap { it?.dependencies?.classpath ?: emptyList() }.distinct()
        toVfsRoots(files)
    }

    private val allScriptsClasspathScope = ClearableLazyValue(cacheLock) {
        NonClasspathDirectoriesScope(getAllScriptsClasspath())
    }

    private val allLibrarySourcesCache = ClearableLazyValue(cacheLock) {
        toVfsRoots(cache.values.flatMap { it?.dependencies?.sources ?: emptyList() }.distinct())
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

    fun getScriptClasspath(file: VirtualFile): List<VirtualFile> = toVfsRoots(getScriptDependencies(file).classpath)

    fun getAllScriptsClasspath(): List<VirtualFile> = allScriptsClasspathCache.get()

    fun getAllLibrarySources(): List<VirtualFile> = allLibrarySourcesCache.get()

    fun getAllScriptsClasspathScope() = allScriptsClasspathScope.get()

    fun getAllLibrarySourcesScope() = allLibrarySourcesScope.get()

    private fun reloadScriptDefinitions() {
        val def = makeScriptDefsFromTemplatesProviderExtensions(project, { ep, ex -> log.warn("[kts] Error loading definition from ${ep.id}", ex) })
        scriptDefinitionProvider.setScriptDefinitions(def)
    }

    private class TimeStampedRequest(val future: CompletableFuture<Unit>, val timeStamp: TimeStamp)

    private class DataAndRequest(
            val dependencies: KotlinScriptExternalDependencies?,
            val modificationStamp: Long?,
            val requestInProgress: TimeStampedRequest? = null
    )

    private val cache = hashMapOf<String, DataAndRequest?>()

    override fun getScriptDependencies(file: VirtualFile): KotlinScriptExternalDependencies = cacheLock.read {
        val path = file.path
        val cached = cache[path]
        cached?.dependencies?.let { return it }

        tryLoadingFromDisk(cached, file, path)

        updateExternalImportsCache(listOf(file))

        return cache[path]?.dependencies ?: NoDependencies
    }

    private fun tryLoadingFromDisk(cached: DataAndRequest?, file: VirtualFile, path: String) {
        if (cached != null) return

        ScriptDependenciesFileAttribute.read(file)?.let { deserialized ->
            save(path, deserialized, file, persist = false)
            invalidateLocalCaches()
            notifyRootsChanged()
        }
    }

    private fun updateExternalImportsCache(files: Iterable<VirtualFile>) = cacheLock.write {
        files.mapNotNull { file ->
            scriptDefinitionProvider.findScriptDefinition(file)?.let {
                updateForFile(file, it)
            }
        }
    }.contains(true)

    private fun updateForFile(file: VirtualFile, scriptDef: KotlinScriptDefinition): Boolean {
        if (!file.isValid) {
            return cache.remove(file.path) != null
        }

        // TODO: support apis that allow for async updates for any template
        if (scriptDef is KotlinScriptDefinitionFromAnnotatedTemplate) {
            return updateAsync(file, scriptDef)
        }
        return updateSync(file, scriptDef)
    }

    private fun updateAsync(file: VirtualFile, scriptDefinition: KotlinScriptDefinitionFromAnnotatedTemplate): Boolean {
        val path = file.path
        val oldDataAndRequest = cache[path]

        if (!shouldSendNewRequest(file, oldDataAndRequest)) {
            return false
        }

        oldDataAndRequest?.requestInProgress?.future?.cancel(true)

        val (currentTimeStamp, newFuture) = sendRequest(path, scriptDefinition, file, oldDataAndRequest)

        cache[path] = DataAndRequest(
                oldDataAndRequest?.dependencies,
                file.modificationStamp,
                TimeStampedRequest(newFuture, currentTimeStamp)
        )
        return false // not changed immediately
    }

    private fun sendRequest(
            path: String,
            scriptDefinition: KotlinScriptDefinitionFromAnnotatedTemplate,
            file: VirtualFile,
            oldDataAndRequest: DataAndRequest?
    ): Pair<TimeStamp, CompletableFuture<Unit>> {
        val currentTimeStamp = TimeStamps.next()

        val newFuture = supplyAsync(Supplier {
            val newDependencies = resolveDependencies(scriptDefinition, file, oldDataAndRequest?.dependencies) ?: EmptyDependencies
            cacheLock.read {
                val lastTimeStamp = cache[path]?.requestInProgress?.timeStamp
                if (lastTimeStamp == currentTimeStamp) {
                    if (cacheSync(newDependencies, oldDataAndRequest?.dependencies, path, file)) {
                        invalidateLocalCaches()
                        notifyRootsChanged()
                    }
                }
            }
        }, threadPool)
        return Pair(currentTimeStamp, newFuture)
    }

    private fun shouldSendNewRequest(file: VirtualFile, oldDataAndRequest: DataAndRequest?): Boolean {
        val currentStamp = file.modificationStamp
        val previousStamp = oldDataAndRequest?.modificationStamp

        if (currentStamp != previousStamp) {
            return true
        }

        return oldDataAndRequest.requestInProgress == null
    }

    private fun updateSync(file: VirtualFile, scriptDef: KotlinScriptDefinition): Boolean {
        val path = file.path
        val oldDeps = cache[path]?.dependencies
        val newDeps = resolveDependencies(scriptDef, file, oldDeps) ?: EmptyDependencies
        return cacheSync(newDeps, oldDeps, path, file)
    }

    private fun cacheSync(
            new: KotlinScriptExternalDependencies,
            old: KotlinScriptExternalDependencies?,
            path: String,
            file: VirtualFile
    ): Boolean {
        return when {
            old == null || !(new.match(old)) -> {
                // changed or new
                save(path, new, file, persist = true)
                true
            }
            else -> {
                save(path, new, file, persist = false)
                // same
                false
            }
        }
    }

    private fun save(path: String, new: KotlinScriptExternalDependencies?, virtualFile: VirtualFile, persist: Boolean) {
        cacheLock.write {
            cache.put(path, DataAndRequest(new, virtualFile.modificationStamp))
        }
        if (persist && new != null) {
            ScriptDependenciesFileAttribute.write(virtualFile, new)
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
        fun updateScriptDependenciesSynchronously(virtualFile: VirtualFile, project: Project) {
            with(getInstance(project)) {
                val scriptDefinition = KotlinScriptDefinitionProvider.getInstance(project)!!.findScriptDefinition(virtualFile)!!
                val updated = updateSync(virtualFile, scriptDefinition)
                assert(updated)
                invalidateLocalCaches()
                notifyRootsChanged()
            }
        }

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

private object EmptyDependencies: KotlinScriptExternalDependencies