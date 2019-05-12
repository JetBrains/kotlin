/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Runnable
import org.jetbrains.kotlin.idea.core.script.ScriptsCompilationConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

// TODO: rename and provide alias for compatibility - this is not only about dependencies anymore
class AsyncScriptDependenciesLoader internal constructor(project: Project) : ScriptDependenciesLoader(project) {
    private val lock = ReentrantReadWriteLock()

    private var notifyRootChange: Boolean = false
    private var backgroundTasksQueue: LoaderBackgroundTask? = null

    override fun isApplicable(file: VirtualFile): Boolean {
        val scriptDefinition = file.findScriptDefinition(project) ?: return false
        return ScriptsCompilationConfigurationUpdater.getInstance(project).isAsyncDependencyResolver(scriptDefinition)
    }

    override fun loadDependencies(file: VirtualFile) {
        lock.write {
            if (backgroundTasksQueue == null) {
                backgroundTasksQueue = LoaderBackgroundTask()
                backgroundTasksQueue!!.addTask(file)
                backgroundTasksQueue!!.start()
            } else {
                backgroundTasksQueue!!.addTask(file)
            }
        }
    }

    override fun shouldShowNotification(): Boolean = !KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled

    override fun notifyRootsChanged(): Boolean {
        lock.write {
            if (notifyRootChange) return false

            if (backgroundTasksQueue == null) {
                submitMakeRootsChange()
                return true
            }

            notifyRootChange = true

            backgroundTasksQueue!!.addOnFinishTask {
                lock.write {
                    notifyRootChange = false
                }
                submitMakeRootsChange()
            }
        }

        return false
    }

    private fun runDependenciesUpdate(file: VirtualFile) {
        val scriptDef = file.findScriptDefinition(project) ?: return

        debug(file) { "start async dependencies loading" }

        val result = refineScriptCompilationConfiguration(VirtualFileScriptSource(file), scriptDef, project)

        debug(file) { "finish async dependencies loading" }

        processRefinedConfiguration(result, file)
    }

    private inner class LoaderBackgroundTask {
        private val sequenceOfFiles: ConcurrentLinkedQueue<VirtualFile> = ConcurrentLinkedQueue()
        private var forceStop : Boolean = false
        private var startedSilently : Boolean = false

        private var onFinish: (() -> Unit)? = null

        fun start() {
            if (shouldShowNotification()) {
                startSilently()
            } else {
                startWithProgress()
            }
        }

        private fun restartWithProgress() {
            forceStop = true
            startWithProgress()
            forceStop = false
        }

        private fun startSilently() {
            startedSilently = true
            BackgroundTaskUtil.executeOnPooledThread(project, Runnable {
                loadDependencies(EmptyProgressIndicator())
            })
        }

        private fun startWithProgress() {
            startedSilently = false
            object : Task.Backgroundable(project, "Kotlin: Loading script dependencies...", true) {
                override fun run(indicator: ProgressIndicator) {
                    loadDependencies(indicator)
                }

            }.queue()
        }

        fun addTask(file: VirtualFile) {
            if (sequenceOfFiles.contains(file)) return

            debug(file) { "added to update queue" }

            sequenceOfFiles.add(file)

            // If the queue is longer than 3, show progress and cancel button
            if (sequenceOfFiles.size > 3 && startedSilently) {
                restartWithProgress()
            }
        }

        fun addOnFinishTask(task: () -> Unit) {
            onFinish = task
        }

        private fun loadDependencies(indicator: ProgressIndicator?) {
            while (true) {
                if (forceStop) return
                if (indicator?.isCanceled == true || sequenceOfFiles.isEmpty()) {
                    lock.write {
                        onFinish?.invoke()
                        backgroundTasksQueue = null
                    }
                    return
                }
                runDependenciesUpdate(sequenceOfFiles.poll())
            }
        }
    }
}


