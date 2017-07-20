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
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.script.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.script.dependencies.DependenciesResolver
import kotlin.script.dependencies.ScriptDependencies
import kotlin.script.dependencies.experimental.AsyncDependenciesResolver

internal class ScriptDependenciesUpdater(
        private val project: Project,
        private val cache: ScriptDependenciesCache,
        private val scriptDefinitionProvider: KotlinScriptDefinitionProvider
) {
    private val requests = ConcurrentHashMap<String, ModStampedRequest>()
    private val contentLoader = ScriptContentLoader(project)
    private val asyncUpdatesDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private val legacyUpdatesDispatcher =
            Executors.newFixedThreadPool(
                    (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
            ).asCoroutineDispatcher()

    init {
        listenToVfsChanges()
    }

    private class TimeStampedJob(val actualJob: Job, val timeStamp: TimeStamp) {
        fun stampBy(virtualFile: VirtualFile) = ModStampedRequest(virtualFile.modificationStamp, this)
    }

    private class ModStampedRequest(
            val modificationStamp: Long,
            val job: TimeStampedJob?
    ) {
        fun cancel() = job?.actualJob?.cancel()
    }

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
            is AsyncDependenciesResolver, is LegacyResolverWrapper -> {
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

        lastRequest?.cancel()

        requests[path] = sendRequest(file, scriptDefinition).stampBy(file)
        return
    }

    private fun shouldSendNewRequest(file: VirtualFile, previousRequest: ModStampedRequest?): Boolean {
        if (previousRequest == null) return true

        return file.modificationStamp != previousRequest.modificationStamp
    }

    private fun sendRequest(
            file: VirtualFile,
            scriptDef: KotlinScriptDefinition
    ): TimeStampedJob {
        val currentTimeStamp = TimeStamps.next()

        val dependenciesResolver = scriptDef.dependencyResolver
        val scriptContents = contentLoader.getScriptContents(scriptDef, file)
        val environment = contentLoader.getEnvironment(scriptDef)
        val newJob = if (dependenciesResolver is AsyncDependenciesResolver) {
            launchAsyncUpdate(asyncUpdatesDispatcher, file, currentTimeStamp, scriptDef) {
                    dependenciesResolver.resolveAsync(scriptContents, environment)
            }
        }
        else {
            assert(dependenciesResolver is LegacyResolverWrapper)
            launchAsyncUpdate(legacyUpdatesDispatcher, file, currentTimeStamp, scriptDef) {
                dependenciesResolver.resolve(scriptContents, environment)
            }
        }
        return TimeStampedJob(newJob, currentTimeStamp)
    }

    private fun launchAsyncUpdate(
            dispatcher: CoroutineDispatcher,
            file: VirtualFile,
            currentTimeStamp: TimeStamp,
            scriptDef: KotlinScriptDefinition,
            doResolve: suspend () -> DependenciesResolver.ResolveResult
    ) = launch(dispatcher) {
        val result = try {
            doResolve()
        }
        catch (t: Throwable) {
            t.asResolveFailure(scriptDef)
        }

        processResult(file, currentTimeStamp, result, scriptDef)
    }

    private fun processResult(
            file: VirtualFile,
            currentTimeStamp: TimeStamp,
            result: DependenciesResolver.ResolveResult,
            scriptDef: KotlinScriptDefinition
    ) {
        val lastRequest = requests[file.path]
        val lastTimeStamp = lastRequest?.job?.timeStamp
        val isLastSentRequest = lastTimeStamp == null || lastTimeStamp == currentTimeStamp
        if (isLastSentRequest) {
            if (lastRequest != null) {
                // no job running atm unless there is a job started while we process this result
                requests.replace(file.path, lastRequest, ModStampedRequest(lastRequest.modificationStamp, job = null))
            }
            ServiceManager.getService(project, ScriptReportSink::class.java)?.attachReports(file, result.reports)
            val resultingDependencies = (result.dependencies ?: ScriptDependencies.Empty).adjustByDefinition(scriptDef)
            if (cache(resultingDependencies, file)) {
                onChange()
            }
        }
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