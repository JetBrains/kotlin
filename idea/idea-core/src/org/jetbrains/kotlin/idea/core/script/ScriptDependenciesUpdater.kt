/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.future.future
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.script.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.script.dependencies.ScriptDependencies
import kotlin.script.dependencies.experimental.AsyncDependenciesResolver

internal class ScriptDependenciesUpdater(
        private val project: Project,
        private val cache: DependenciesCache,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider
) {
    private val requests = ConcurrentHashMap<String, ModStampedRequest>()
    private val scriptDependencyUpdatesDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private val contentLoader = ScriptContentLoader(project)

    init {
        listenToVfsChanges()
    }

    private class TimeStampedRequest(val future: CompletableFuture<*>, val timeStamp: TimeStamp)

    private class ModStampedRequest(
            val modificationStamp: Long,
            val request: TimeStampedRequest? = null
    )

    fun getCurrentDependencies(file: VirtualFile): ScriptDependencies {
        cache[file]?.let { return it }

        tryLoadingFromDisk(file)

        updateCache(listOf(file))

        return cache[file] ?: ScriptDependencies.Empty
    }

    private fun tryLoadingFromDisk(file: VirtualFile) {
        ScriptDependenciesFileAttribute.read(file)?.let { deserialized ->
            cache.save(file, deserialized)
            onChange()
        }
    }

    private fun updateCache(files: Iterable<VirtualFile>) =
            files.map { file ->
                if (!file.isValid) {
                    return cache.delete(file)
                }
                else {
                    updateForFile(file)
                }
            }.contains(true)

    private fun updateForFile(file: VirtualFile): Boolean {
        val scriptDef = scriptDefinitionProvider.findScriptDefinition(file) ?: return false

        return when (scriptDef.dependencyResolver) {
            is AsyncDependenciesResolver -> {
                updateAsync(file, scriptDef)
                return false
            }
            else -> updateSync(file, scriptDef)
        }
    }

    private fun updateAsync(
            file: VirtualFile,
            scriptDefinition: KotlinScriptDefinition
    ) {
        val path = file.path
        val lastRequest = requests[path]

        if (!shouldSendNewRequest(file, lastRequest)) {
            return
        }

        lastRequest?.request?.future?.cancel(true)

        val (currentTimeStamp, newFuture) = sendRequest(file, scriptDefinition)

        requests[path] = ModStampedRequest(
                file.modificationStamp,
                TimeStampedRequest(newFuture, currentTimeStamp)
        )
        return
    }

    private fun shouldSendNewRequest(file: VirtualFile, previousRequest: ModStampedRequest?): Boolean {
        val currentStamp = file.modificationStamp
        val previousStamp = previousRequest?.modificationStamp

        if (currentStamp != previousStamp) {
            return true
        }

        return previousRequest.request == null
    }

    private fun sendRequest(
            file: VirtualFile,
            scriptDef: KotlinScriptDefinition
    ): Pair<TimeStamp, CompletableFuture<*>> {
        val currentTimeStamp = TimeStamps.next()
        val dependenciesResolver = scriptDef.dependencyResolver as AsyncDependenciesResolver
        val path = file.path

        val newFuture = future(scriptDependencyUpdatesDispatcher) {
            dependenciesResolver.resolveAsync(
                    contentLoader.getScriptContents(scriptDef, file),
                    (scriptDef as? KotlinScriptDefinitionFromAnnotatedTemplate)?.environment.orEmpty()
            )
        }.thenAccept { result ->
            val lastTimeStamp = requests[path]?.request?.timeStamp
            if (lastTimeStamp == currentTimeStamp) {
                ServiceManager.getService(project, ScriptReportSink::class.java)?.attachReports(file, result.reports)
                if (cache(result.dependencies ?: ScriptDependencies.Empty, file)) {
                    onChange()
                }
            }
        }
        return Pair(currentTimeStamp, newFuture)
    }

    fun updateSync(file: VirtualFile, scriptDef: KotlinScriptDefinition): Boolean {
        val newDeps = contentLoader.loadContentsAndResolveDependencies(scriptDef, file) ?: ScriptDependencies.Empty
        return cache(newDeps, file)
    }

    private fun cache(
            new: ScriptDependencies,
            file: VirtualFile
    ): Boolean {
        val updated = cache.save(file, new)
        if (updated) {
            ScriptDependenciesFileAttribute.write(file, new)
        }
        return updated
    }

    fun onChange() {
        cache.onChange()
        notifyRootsChanged()
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

    private fun listenToVfsChanges() {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {

            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            val application = ApplicationManager.getApplication()

            override fun after(events: List<VFileEvent>) {
                if (updateCache(events.mapNotNull {
                    // The check is partly taken from the BuildManager.java
                    it.file?.takeIf {
                        // the isUnitTestMode check fixes ScriptConfigurationHighlighting & Navigation tests, since they are not trigger proper update mechanims
                        // TODO: find out the reason, then consider to fix tests and remove this check
                        (application.isUnitTestMode || projectFileIndex.isInContent(it)) && !ProjectUtil.isProjectOrWorkspaceFile(it)
                    }
                })) {
                    onChange()
                }
            }
        })
    }

    fun clear() {
        cache.clear()
        requests.clear()
    }
}


private data class TimeStamp(private val stamp: Long) {
    operator fun compareTo(other: TimeStamp) = this.stamp.compareTo(other.stamp)
}

private object TimeStamps {
    private var current: Long = 0

    fun next() = TimeStamp(current++)
}