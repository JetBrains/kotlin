/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptContentLoader
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import org.jetbrains.kotlin.scripting.resolve.adjustByDefinition
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies

abstract class ScriptDependenciesLoader(protected val project: Project) {

    abstract fun isApplicable(file: VirtualFile): Boolean
    abstract fun loadDependencies(file: VirtualFile)

    protected abstract fun shouldShowNotification(): Boolean

    protected var shouldNotifyRootsChanged = false

    protected val contentLoader = ScriptContentLoader(project)
    protected val cache: ScriptDependenciesCache = ServiceManager.getService(project, ScriptDependenciesCache::class.java)

    private val reporter: ScriptReportSink = ServiceManager.getService(project, ScriptReportSink::class.java)

    protected fun processResult(result: DependenciesResolver.ResolveResult, file: VirtualFile, scriptDef: KotlinScriptDefinition) {
        debug(file) { "dependencies from ${this.javaClass} received = $result" }

        if (cache[file] == null) {
            saveDependencies(result, file, scriptDef)
            attachReportsIfChanged(result, file, scriptDef)
            return
        }

        val newDependencies = result.dependencies?.adjustByDefinition(scriptDef)
        if (cache[file] != newDependencies) {
            if (shouldShowNotification() && !ApplicationManager.getApplication().isUnitTestMode) {
                debug(file) {
                    "dependencies changed, notification was shown: old = ${cache[file]}, new = $newDependencies"
                }
                file.addScriptDependenciesNotificationPanel(result, project) {
                    saveDependencies(it, file, scriptDef)
                    attachReportsIfChanged(it, file, scriptDef)
                    submitMakeRootsChange()
                }
            } else {
                debug(file) {
                    "dependencies changed, new dependencies were applied automatically: old = ${cache[file]}, new = $newDependencies"
                }
                saveDependencies(result, file, scriptDef)
                attachReportsIfChanged(result, file, scriptDef)
            }
        } else {
            attachReportsIfChanged(result, file, scriptDef)

            if (shouldShowNotification()) {
                file.removeScriptDependenciesNotificationPanel(project)
            }
        }
    }

    private fun attachReportsIfChanged(result: DependenciesResolver.ResolveResult, file: VirtualFile, scriptDef: KotlinScriptDefinition) {
        if (file.getUserData(IdeScriptReportSink.Reports) != result.reports.takeIf { it.isNotEmpty() }) {
            reporter.attachReports(file, result.reports)
        }
    }

    private fun saveDependencies(result: DependenciesResolver.ResolveResult, file: VirtualFile, scriptDef: KotlinScriptDefinition) {
        if (shouldShowNotification()) {
            file.removeScriptDependenciesNotificationPanel(project)
        }

        val dependencies = result.dependencies?.adjustByDefinition(scriptDef) ?: return
        saveToCache(file, dependencies)
    }

    protected fun saveToCache(file: VirtualFile, dependencies: ScriptDependencies) {
        val rootsChanged = cache.hasNotCachedRoots(dependencies)
        if (cache.save(file, dependencies)) {
            debug(file) {
                "dependencies were saved to file attributes: dependencies = $dependencies"
            }
            file.scriptDependencies = dependencies
        }

        if (rootsChanged) {
            shouldNotifyRootsChanged = true
        }
    }

    open fun notifyRootsChanged(): Boolean = submitMakeRootsChange()

    protected fun submitMakeRootsChange(): Boolean {
        if (!shouldNotifyRootsChanged) return false

        val doNotifyRootsChanged = Runnable {
            runWriteAction {
                if (project.isDisposed) return@runWriteAction

                debug(null) { "root change event for ${this.javaClass}" }

                shouldNotifyRootsChanged = false
                ProjectRootManagerEx.getInstanceEx(project)?.makeRootsChange(EmptyRunnable.getInstance(), false, true)
                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
            }
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            TransactionGuard.submitTransaction(project, doNotifyRootsChanged)
        } else {
            TransactionGuard.getInstance().submitTransactionLater(project, doNotifyRootsChanged)
        }

        return true
    }

    companion object {
        private val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.script")

        internal fun debug(file: VirtualFile? = null, message: () -> String) {
            if (LOG.isDebugEnabled) {
                LOG.debug("[KOTLIN SCRIPT] " + (file?.let { "file = ${file.path}, " } ?: "") + message())
            }
        }
    }
}
