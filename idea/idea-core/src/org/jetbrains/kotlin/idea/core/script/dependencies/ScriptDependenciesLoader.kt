/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.SLRUMap
import kotlinx.coroutines.experimental.launch
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesCache
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.scriptDependencies
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.script.*
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.ScriptReport

abstract class ScriptDependenciesLoader(
    protected val file: VirtualFile,
    protected val scriptDef: KotlinScriptDefinition,
    protected val project: Project,
    private val shouldNotifyRootsChanged: Boolean
) {
    companion object {
        private val loaders = SLRUMap<VirtualFile, ScriptDependenciesLoader>(10, 10)

        fun updateDependencies(
            file: VirtualFile,
            scriptDef: KotlinScriptDefinition,
            project: Project,
            shouldNotifyRootsChanged: Boolean
        ) {
            val existingLoader = loaders[file]
            if (existingLoader != null) return existingLoader.updateDependencies()

            val newLoader = when (scriptDef.dependencyResolver) {
                is AsyncDependenciesResolver,
                is LegacyResolverWrapper -> AsyncScriptDependenciesLoader(file, scriptDef, project)
                else -> SyncScriptDependenciesLoader(file, scriptDef, project, shouldNotifyRootsChanged)
            }
            loaders.put(file, newLoader)
            newLoader.updateDependencies()
        }
    }

    fun updateDependencies() {
        if (shouldUseBackgroundThread()) {
            object : Task.Backgroundable(project, "Kotlin: Loading dependencies for ${file.name} ...", true) {
                override fun run(indicator: ProgressIndicator) {
                    loadDependencies()
                }
            }.queue()
        } else {
            loadDependencies()
        }
    }

    protected abstract fun loadDependencies()
    protected abstract fun shouldUseBackgroundThread(): Boolean

    protected val contentLoader = ScriptContentLoader(project)
    protected val cache: ScriptDependenciesCache = ServiceManager.getService(project, ScriptDependenciesCache::class.java)

    protected fun processResult(result: DependenciesResolver.ResolveResult) {
        saveDependencies(result)
    }

    private fun saveDependencies(result: DependenciesResolver.ResolveResult) {
        ServiceManager.getService(project, ScriptReportSink::class.java)?.attachReports(file, result.reports)

        val newDependencies = result.dependencies?.adjustByDefinition(scriptDef) ?: ScriptDependencies.Empty

        val rootsChanged = cache.hasNotCachedRoots(newDependencies)
        if (cache.save(file, newDependencies)) {
            if (result.reports.any { it.severity == ScriptReport.Severity.FATAL }) {
                file.scriptDependencies = null
            } else {
                file.scriptDependencies = newDependencies
            }
        }

        if (rootsChanged) {
            notifyRootsChanged()
        }

        loaders.remove(file)
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    protected fun notifyRootsChanged() {
        if (!shouldNotifyRootsChanged) return

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
}
