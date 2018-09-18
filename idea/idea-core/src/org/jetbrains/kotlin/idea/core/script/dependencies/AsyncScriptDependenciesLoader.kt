/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.*
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.cancelOnDisposal
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.LegacyResolverWrapper
import org.jetbrains.kotlin.script.asResolveFailure
import java.util.concurrent.Executors
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class AsyncScriptDependenciesLoader(
    file: VirtualFile,
    scriptDef: KotlinScriptDefinition,
    project: Project
) : ScriptDependenciesLoader(file, scriptDef, project, true) {

    override fun loadDependencies() {
        if (!shouldSendNewRequest(lastRequest)) {
            return
        }

        lastRequest?.cancel()
        lastRequest = sendRequest().stampBy(file)

        if (shouldUseBackgroundThread()) {
            runBlocking {
                lastRequest?.job?.actualJob?.join()
            }
        }
    }

    override fun shouldUseBackgroundThread() = KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled
    override fun shouldShowNotification(): Boolean = !KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled

    private var lastRequest: ModStampedRequest? = null

    private val asyncUpdatesDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
    private val legacyUpdatesDispatcher =
        Executors.newFixedThreadPool(
            (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        ).asCoroutineDispatcher()

    private fun shouldSendNewRequest(previousRequest: ModStampedRequest?): Boolean {
        if (previousRequest == null) return true
        return file.modificationStamp != previousRequest.modificationStamp
    }

    private fun sendRequest(): TimeStampedJob {
        val currentTimeStamp = TimeStamps.next()

        val dependenciesResolver = scriptDef.dependencyResolver
        val scriptContents = contentLoader.getScriptContents(scriptDef, file)
        val environment = contentLoader.getEnvironment(scriptDef)
        val newJob = if (dependenciesResolver is AsyncDependenciesResolver) {
            launchAsyncUpdate(asyncUpdatesDispatcher, currentTimeStamp) {
                dependenciesResolver.resolveAsync(scriptContents, environment)
            }
        } else {
            assert(dependenciesResolver is LegacyResolverWrapper)
            launchAsyncUpdate(legacyUpdatesDispatcher, currentTimeStamp) {
                dependenciesResolver.resolve(scriptContents, environment)
            }
        }

        return TimeStampedJob(newJob, currentTimeStamp)
    }

    private fun launchAsyncUpdate(
        dispatcher: CoroutineDispatcher,
        currentTimeStamp: TimeStamp,
        doResolve: suspend () -> DependenciesResolver.ResolveResult
    ): Job = launch(dispatcher + project.cancelOnDisposal) {
        val result = try {
            doResolve()
        } catch (t: Throwable) {
            t.asResolveFailure(scriptDef)
        }

        processResult(currentTimeStamp, result)
    }

    private fun processResult(
        currentTimeStamp: TimeStamp,
        result: DependenciesResolver.ResolveResult
    ) {
        val lastTimeStamp = lastRequest?.job?.timeStamp
        val isLastSentRequest = lastTimeStamp == null || lastTimeStamp == currentTimeStamp
        if (isLastSentRequest) {
            if (lastRequest != null) {
                // no job running atm unless there is a job started while we process this result
                lastRequest = ModStampedRequest(lastRequest!!.modificationStamp, job = null)
            }

            processResult(result)
        }
    }

    private class ModStampedRequest(val modificationStamp: Long, val job: TimeStampedJob?) {
        fun cancel() = job?.actualJob?.cancel()
    }

    private class TimeStampedJob(val actualJob: Job, val timeStamp: TimeStamp) {
        fun stampBy(virtualFile: VirtualFile) =
            ModStampedRequest(
                virtualFile.modificationStamp,
                this
            )
    }

    private data class TimeStamp(private val stamp: Long) {
        operator fun compareTo(other: TimeStamp) = this.stamp.compareTo(other.stamp)
    }

    private object TimeStamps {
        private var current: Long = 0

        fun next() =
            TimeStamp(current++)
    }
}


