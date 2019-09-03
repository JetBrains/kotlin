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
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.valueOrNull

// TODO: rename and provide alias for compatibility - this is not only about dependencies anymore
abstract class ScriptDependenciesLoader(protected val project: Project) {

    abstract fun isApplicable(file: VirtualFile, scriptDefinition: ScriptDefinition): Boolean
    abstract fun loadDependencies(file: VirtualFile, scriptDefinition: ScriptDefinition)

    protected abstract fun shouldShowNotification(): Boolean

    protected var shouldNotifyRootsChanged = false

    protected val cache: ScriptsCompilationConfigurationCache = ServiceManager.getService(project, ScriptsCompilationConfigurationCache::class.java)

    private val reporter: ScriptReportSink = ServiceManager.getService(project, ScriptReportSink::class.java)

    protected fun processRefinedConfiguration(result: ScriptCompilationConfigurationResult, file: VirtualFile) {
        debug(file) { "refined script compilation configuration from ${this.javaClass} received = $result" }

        val oldResult = cache[file]

        if (oldResult == null) {
            save(result, file)
            attachReportsIfChanged(result, file)
            return
        }

        if (oldResult != result) {
            if (shouldShowNotification()
                && oldResult.valueOrNull() != result.valueOrNull() // Only compilation configuration changed
                && !ApplicationManager.getApplication().isUnitTestMode
            ) {
                debug(file) {
                    "dependencies changed, notification is shown: old = $oldResult, new = $result"
                }
                file.addScriptDependenciesNotificationPanel(result, project) {
                    save(it, file)
                    attachReportsIfChanged(result, file)
                    submitMakeRootsChange()
                }
            } else {
                debug(file) {
                    "dependencies changed, new dependencies are applied automatically: old = $oldResult, new = $result"
                }
                save(result, file)
                attachReportsIfChanged(result, file)
            }
        } else {
            attachReportsIfChanged(result, file)

            if (shouldShowNotification()) {
                file.removeScriptDependenciesNotificationPanel(project)
            }
        }
    }

    private fun attachReportsIfChanged(result: ResultWithDiagnostics<*>, file: VirtualFile) {
        reporter.attachReports(file, result.reports)
    }

    private fun save(compilationConfigurationResult: ScriptCompilationConfigurationResult?, file: VirtualFile) {
        if (shouldShowNotification()) {
            file.removeScriptDependenciesNotificationPanel(project)
        }
        if (compilationConfigurationResult != null) {
            saveToCache(file, compilationConfigurationResult)
        }
    }

    protected fun saveToCache(
        file: VirtualFile, compilationConfigurationResult: ScriptCompilationConfigurationResult, skipSaveToAttributes: Boolean = false
    ) {
        val rootsChanged = compilationConfigurationResult.valueOrNull()?.let { cache.hasNotCachedRoots(it) } ?: false
        if (cache.save(file, compilationConfigurationResult)
            && !skipSaveToAttributes
            && compilationConfigurationResult is ResultWithDiagnostics.Success
        ) {
            debug(file) {
                "refined configuration is saved to file attributes: $compilationConfigurationResult"
            }
            if (compilationConfigurationResult.value is ScriptCompilationConfigurationWrapper.FromLegacy)
                file.scriptDependencies = compilationConfigurationResult.value.legacyDependencies
            else
                file.scriptCompilationConfiguration = compilationConfigurationResult.value.configuration
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
