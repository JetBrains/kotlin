/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.LegacyResolverWrapper
import org.jetbrains.kotlin.script.asResolveFailure
import org.jetbrains.kotlin.scripting.compiler.plugin.definitions.findScriptDefinition
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver

class AsyncScriptDependenciesLoader internal constructor(project: Project) : ScriptDependenciesLoader(project) {
    private val backgroundTaskLock = ReentrantReadWriteLock()
    private var backgroundTasksQueue: LoaderBackgroundTask? = null

    override fun loadDependencies(file: VirtualFile, scriptDef: KotlinScriptDefinition) {
        backgroundTaskLock.write {
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

    private fun runDependenciesUpdate(file: VirtualFile) {
        val scriptDef = runReadAction { file.findScriptDefinition(project) } ?: return
        // runBlocking is using there to avoid loading dependencies asynchronously
        // because it leads to starting more than one gradle daemon in case of resolving dependencies in build.gradle.kts
        // It is more efficient to use one hot daemon consistently than multiple daemon in parallel
        val result = runBlocking {
            try {
                resolveDependencies(file, scriptDef)
            } catch (t: Throwable) {
                t.asResolveFailure(scriptDef)
            }
        }
        processResult(result, file, scriptDef)
    }

    private suspend fun resolveDependencies(file: VirtualFile, scriptDef: KotlinScriptDefinition): DependenciesResolver.ResolveResult {
        val dependenciesResolver = scriptDef.dependencyResolver
        val scriptContents = contentLoader.getScriptContents(scriptDef, file)
        val environment = contentLoader.getEnvironment(scriptDef)
        return if (dependenciesResolver is AsyncDependenciesResolver) {
            dependenciesResolver.resolveAsync(scriptContents, environment)
        } else {
            assert(dependenciesResolver is LegacyResolverWrapper)
            dependenciesResolver.resolve(scriptContents, environment)
        }
    }

    private inner class LoaderBackgroundTask {
        private val sequenceOfFiles: ConcurrentLinkedQueue<VirtualFile> = ConcurrentLinkedQueue()

        fun start() {
            if (shouldShowNotification()) {
                BackgroundTaskUtil.executeOnPooledThread(project, Runnable {
                    loadDependencies(null)
                })
            } else {
                object : Task.Backgroundable(project, "Kotlin: Loading script dependencies...", true) {
                    override fun run(indicator: ProgressIndicator) {
                        loadDependencies(indicator)
                    }

                }.queue()
            }
        }

        fun addTask(file: VirtualFile) {
            sequenceOfFiles.add(file)
        }

        private fun loadDependencies(indicator: ProgressIndicator?) {
            while (true) {
                if (indicator?.isCanceled == true || sequenceOfFiles.isEmpty()) {
                    backgroundTaskLock.write {
                        backgroundTasksQueue = null
                    }
                    return
                }
                runDependenciesUpdate(sequenceOfFiles.poll())
            }
        }
    }
}


