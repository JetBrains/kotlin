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

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.isProjectOrWorkspaceFile
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.StatusBarEx
import com.intellij.openapi.wm.ex.WindowManagerEx
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.core.util.cancelOnDisposal
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import org.jetbrains.kotlin.script.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.ScriptReport

class ScriptDependenciesUpdater(
    private val project: Project,
    private val cache: ScriptDependenciesCache,
    private val scriptDefinitionProvider: ScriptDefinitionProvider
) {
    private val requests = ConcurrentHashMap<String, ModStampedRequest>()
    private val contentLoader = ScriptContentLoader(project)
    private val asyncUpdatesDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private val legacyUpdatesDispatcher =
        Executors.newFixedThreadPool(
            (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        ).asCoroutineDispatcher()

    private val modifiedScripts = mutableSetOf<VirtualFile>()

    init {
        listenToVfsChanges()
    }

    private class TimeStampedJob(val actualJob: Task.Backgroundable, val timeStamp: TimeStamp) {
        fun stampBy(virtualFile: VirtualFile) = ModStampedRequest(virtualFile.modificationStamp, this)
    }

    private class ModStampedRequest(
        val modificationStamp: Long,
        val job: TimeStampedJob?
    ) {
        fun cancel() {
            val actualJob = job?.actualJob ?: return
            val frame = (WindowManager.getInstance() as? WindowManagerEx)?.findFrameFor(actualJob.project)
            val statusBar = frame?.statusBar as? StatusBarEx ?: return
            statusBar.backgroundProcesses.find { it.first == actualJob }?.second?.cancel()
        }
    }

    fun getCurrentDependencies(file: VirtualFile): ScriptDependencies {
        cache[file]?.let { return it }

        tryLoadingFromDisk(file)
        performUpdate(file)

        return cache[file] ?: ScriptDependencies.Empty
    }

    fun reloadModifiedScripts() {
        for (it in modifiedScripts.filter { cache[it] != null }) {
            performUpdate(it)
        }
        modifiedScripts.clear()
    }

    private fun tryLoadingFromDisk(file: VirtualFile): Boolean {
        val deserializedDependencies = file.scriptDependencies ?: return false
        saveToCache(deserializedDependencies, file)
        return true
    }

    private fun saveToCache(deserialized: ScriptDependencies, file: VirtualFile) {
        val rootsChanged = cache.hasNotCachedRoots(deserialized)
        cache.save(file, deserialized)
        if (rootsChanged) {
            notifyRootsChanged()
        }
    }

    fun requestUpdate(files: Iterable<VirtualFile>) {
        files.forEach { file ->
            if (!file.isValid) {
                cache.delete(file)
            } else if (cache[file] != null) { // only update dependencies for scripts that were touched recently
                modifiedScripts.add(file)
            }
        }
    }

    private fun performUpdate(file: VirtualFile) {
        val scriptDef = scriptDefinitionProvider.findScriptDefinition(file) ?: return
        when (scriptDef.dependencyResolver) {
            is AsyncDependenciesResolver, is LegacyResolverWrapper -> {
                updateAsync(file, scriptDef)
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

        val newJob = object : Task.Backgroundable(project, "Kotlin: Loading dependencies for ${file.name} ...", true) {
            override fun run(indicator: ProgressIndicator) {
                if (updateSync(file, scriptDef)) {
                    notifyRootsChanged()
                }
            }
        }
        newJob.queue()
        return TimeStampedJob(newJob, currentTimeStamp)
    }

    private fun launchAsyncUpdate(
        dispatcher: CoroutineDispatcher,
        file: VirtualFile,
        currentTimeStamp: TimeStamp,
        scriptDef: KotlinScriptDefinition,
        doResolve: suspend () -> DependenciesResolver.ResolveResult
    ) = launch(dispatcher + project.cancelOnDisposal) {
        val result = try {
            doResolve()
        } catch (t: Throwable) {
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
            val resultingDependencies = result.dependencies?.adjustByDefinition(scriptDef) ?: return
            if (saveNewDependencies(resultingDependencies, file, result.reports.any { it.severity == ScriptReport.Severity.FATAL })) {
                notifyRootsChanged()
            }
        }
    }


    fun updateSync(file: VirtualFile, scriptDef: KotlinScriptDefinition): Boolean {
        val result = contentLoader.loadContentsAndResolveDependencies(scriptDef, file)
        val newDeps = result.dependencies?.adjustByDefinition(scriptDef) ?: ScriptDependencies.Empty
        return saveNewDependencies(newDeps, file, result.reports.any { it.severity == ScriptReport.Severity.FATAL })
    }

    private fun saveNewDependencies(
        new: ScriptDependencies,
        file: VirtualFile,
        hasFatalErrors: Boolean
    ): Boolean {
        val rootsChanged = cache.hasNotCachedRoots(new)
        if (cache.save(file, new)) {
            if (hasFatalErrors) {
                file.scriptDependencies = null
            } else {
                file.scriptDependencies = new
            }
        }
        return rootsChanged
    }

    fun notifyRootsChanged() {
        val rootsChangesRunnable = {
            runWriteAction {
                if (project.isDisposed) return@runWriteAction

                ProjectRootManagerEx.getInstanceEx(project)?.makeRootsChange(EmptyRunnable.getInstance(), false, true)
                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
            }
        }

        val application = ApplicationManager.getApplication()
        if (application.isUnitTestMode) {
            rootsChangesRunnable()
        } else {
            launch(EDT(project)) {
                rootsChangesRunnable()
            }
        }
    }

    private fun listenToVfsChanges() {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {
            val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            val application = ApplicationManager.getApplication()

            override fun after(events: List<VFileEvent>) {
                if (application.isUnitTestMode && application.isScriptDependenciesUpdaterDisabled == true) {
                    return
                }

                val modifiedScripts = events.mapNotNull {
                    // The check is partly taken from the BuildManager.java
                    it.file?.takeIf {
                        // the isUnitTestMode check fixes ScriptConfigurationHighlighting & Navigation tests, since they are not trigger proper update mechanims
                        // TODO: find out the reason, then consider to fix tests and remove this check
                        (application.isUnitTestMode ||
                                scriptDefinitionProvider.isScript(it.name) && projectFileIndex.isInContent(it)) && !isProjectOrWorkspaceFile(it)
                    }
                }
                requestUpdate(modifiedScripts)
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

@set: TestOnly
var Application.isScriptDependenciesUpdaterDisabled by NotNullableUserDataProperty(
    Key.create("SCRIPT_DEPENDENCIES_UPDATER_DISABLED"),
    false
)

interface DefaultScriptDependenciesProvider {
    fun defaultDependenciesFor(scriptFile: VirtualFile): ScriptDependencies?

    companion object : ProjectExtensionDescriptor<DefaultScriptDependenciesProvider>(
        "org.jetbrains.kotlin.defaultScriptDependenciesProvider",
        DefaultScriptDependenciesProvider::class.java
    )

}