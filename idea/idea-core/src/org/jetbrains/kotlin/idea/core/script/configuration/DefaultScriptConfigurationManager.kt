/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptConfigurationManagerExtensions.LOADER
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptConfigurationManagerExtensions.LISTENER
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationFileAttributeCache
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationMemoryCache
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.core.script.configuration.listener.DefaultScriptChangeListener
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangesNotifier
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptOutsiderFileConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.utils.BackgroundExecutor
import org.jetbrains.kotlin.idea.core.script.configuration.utils.DefaultBackgroundExecutor
import org.jetbrains.kotlin.idea.core.script.configuration.utils.TestingBackgroundExecutor
import org.jetbrains.kotlin.idea.core.script.configuration.utils.isUnitTestModeWithoutScriptLoadingNotification
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.script.experimental.api.ScriptDiagnostic

/**
 * Standard implementation of scripts configuration loading and caching
 * (we have plans to extract separate implementation for Gradle scripts).
 *
 * ## Loading initiation
 *
 * [getConfiguration] will be called when we need to show or analyze some script file.
 *
 * As described in [AbstractScriptConfigurationManager], configuration may be loaded from [cache]
 * or [reloadOutOfDateConfiguration] will be called on [cache] miss.
 *
 * There are 2 tiers [cache]: memory and FS. For now FS cache implemented by [ScriptConfigurationLoader]
 * because we are not storing classpath roots yet. As a workaround cache.all() will return only memory
 * cached configurations.  So, for now we are indexing roots that loaded from FS with
 * default [reloadOutOfDateConfiguration] mechanics. todo(KT-34444): implement fs classpath roots cache
 *
 * [notifier] will call first applicable [listeners] when editor is activated or document changed.
 * Listener may call [updater] to invalidate configuration and schedule reloading.
 *
 * Also, [ScriptConfigurationUpdater.ensureConfigurationUpToDate] may be called from [UnusedSymbolInspection]
 * to ensure that configuration of all scripts containing some symbol are up-to-date or try load it in sync.
 * Note: it makes sense only in case of "auto apply" mode and sync loader, in other cases all symbols just
 * will be treated as used.
 *
 * ## Loading
 *
 * When requested, configuration will be loaded using first applicable [loaders].
 * It can work synchronously or asynchronously.
 *
 * Synchronous loader will be called just immediately. Despite this, its result may not be applied immediately,
 * see next section for details.
 *
 * Asynchronous loader will be called in background thread (by [BackgroundExecutor]).
 *
 * ## Applying
 *
 * By default loaded configuration will *not* be applied immediately. Instead, we show in editor notification
 * that suggests user to apply changed configuration. This was done to avoid sporadically starting indexing of new roots,
 * which may happens regularly for large Gradle projects.
 *
 * Notification will be displayed when configuration is going to be updated. First configuration will be loaded
 * without notification.
 *
 * This behavior may be disabled by enabling "auto reload" in project settings.
 * When enabled, all loaded configurations will be applied immediately, without any notification.
 *
 * ## Concurrency
 *
 * Each files may be in on of this state:
 * - scriptDefinition is not ready
 * - not loaded
 * - up-to-date
 * - invalid, in queue (in [BackgroundExecutor] queue)
 * - invalid, loading
 * - invalid, waiting for apply
 *
 * [reloadOutOfDateConfiguration] guard this states. See it's docs for more details.
 */
internal class DefaultScriptConfigurationManager(project: Project) :
    AbstractScriptConfigurationManager(project) {

    internal val backgroundExecutor: BackgroundExecutor =
        if (ApplicationManager.getApplication().isUnitTestMode) TestingBackgroundExecutor(rootsIndexer)
        else DefaultBackgroundExecutor(project, rootsIndexer)

    private val outsiderLoader = ScriptOutsiderFileConfigurationLoader(project)
    private val fileAttributeCache = ScriptConfigurationFileAttributeCache(project)
    private val defaultLoader = DefaultScriptConfigurationLoader(project)
    private val loaders: Sequence<ScriptConfigurationLoader>
        get() = sequence {
            yield(outsiderLoader)
            yield(fileAttributeCache)
            yieldAll(LOADER.getPoint(project).extensionList)
            yield(defaultLoader)
        }

    private val defaultListener = DefaultScriptChangeListener()
    private val listeners: Sequence<ScriptChangeListener>
        get() = sequence {
            yieldAll(LISTENER.getPoint(project).extensionList)
            yield(defaultListener)
        }

    private val notifier = ScriptChangesNotifier(project, updater, listeners)

    private val saveLock = ReentrantLock()

    override fun createCache() = object : ScriptConfigurationMemoryCache(project) {
        override fun setLoaded(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot) {
            super.setLoaded(file, configurationSnapshot)
            fileAttributeCache.save(file, configurationSnapshot.configuration)
        }
    }

    /**
     * Will be called on [cache] miss to initiate loading of [file]'s script configuration.
     *
     * ## Concurrency
     *
     * Each files may be in on of the states described below:
     * - scriptDefinition is not ready. `ScriptDefinitionsManager.getInstance(project).isReady() == false`.
     * [clearConfigurationCachesAndRehighlight] will be called when [ScriptDefinitionsManager] will be ready
     * which will call [reloadOutOfDateConfiguration] for opened editors.
     * - unknown. When [isFirstLoad] true (`cache[file] == null`).
     * - up-to-date. `cache[file]?.upToDate == true`.
     * - invalid, in queue. `cache[file]?.upToDate == false && file in backgroundExecutor`.
     * - invalid, loading. `cache[file]?.upToDate == false && file !in backgroundExecutor`.
     * - invalid, waiting for apply. `cache[file]?.upToDate == false && file !in backgroundExecutor` and has notification panel?
     *
     * Async:
     * - up-to-date: [reloadOutOfDateConfiguration] will not be called.
     * - `unknown` and `invalid, in queue`:
     *   Concurrent async loading will be guarded by `backgroundExecutor.ensureScheduled`
     *   (only one task per file will be scheduled at same time)
     * - `invalid, loading`
     *   Loading should be removed from `backgroundExecutor`, and will be rescheduled on change
     *   and file will be up-to-date checked again. This will happen after current loading,
     *   because only `backgroundExecutor` execute tasks in one thread.
     * - `invalid, waiting for apply`:
     *   Loading will not be queued, since we are marking file as up-to-date with
     *   not yet applied configuration.
     *
     * Sync:
     * - up-to-date:
     *   [reloadOutOfDateConfiguration] will not be called.
     * - all other states, i.e: `unknown`, `invalid, in queue`, `invalid, loading` and `invalid, ready for apply`:
     *   everything will be computed just in place, possible concurrently.
     *   [suggestOrSaveConfiguration] calls will be serialized by the [saveLock]
     */
    override fun reloadOutOfDateConfiguration(
        file: KtFile,
        isFirstLoad: Boolean,
        loadEvenWillNotBeApplied: Boolean,
        forceSync: Boolean
    ) {
        val virtualFile = file.originalFile.virtualFile ?: return

        val autoReloadEnabled = KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled
        val shouldLoad = isFirstLoad || loadEvenWillNotBeApplied || autoReloadEnabled
        if (!shouldLoad) return

        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return
        val scriptDefinition = file.findScriptDefinition() ?: return

        val (async, sync) = loaders.partition { it.shouldRunInBackground(scriptDefinition) }

        val syncLoader = sync.firstOrNull { it.loadDependencies(isFirstLoad, file, scriptDefinition, loadingContext) }
        if (syncLoader == null) {
            // run async loader
            if (forceSync) {
                async.first { it.loadDependencies(isFirstLoad, file, scriptDefinition, loadingContext) }
            } else {
                backgroundExecutor.ensureScheduled(virtualFile) {
                    val cached = getCachedConfigurationState(virtualFile)

                    val applied = cached?.applied
                    if (applied != null && applied.inputs.isUpToDate(project, virtualFile)) {
                        // in case user reverted to applied configuration
                        suggestOrSaveConfiguration(virtualFile, applied, false)
                    } else if (cached == null || !cached.isUpToDate(project, virtualFile)) {
                        // don't start loading if nothing was changed
                        // (in case we checking for up-to-date and loading concurrently)
                        val actualIsFirstLoad = cached == null
                        async.first { it.loadDependencies(actualIsFirstLoad, file, scriptDefinition, loadingContext) }
                    }
                }
            }
        }
    }

    /**
     * Save configurations into cache.
     * Start indexing for new class/source roots.
     * Re-highlight opened scripts with changed configuration.
     */
    override fun saveCompilationConfigurationAfterImport(files: List<Pair<VirtualFile, ScriptConfigurationSnapshot>>) {
        rootsIndexer.transaction {
            for ((file, result) in files) {
                loadingContext.saveNewConfiguration(file, result)
            }
        }
    }

    private val loadingContext = object : ScriptConfigurationLoadingContext {
        override fun getCachedConfiguration(file: VirtualFile): ScriptConfigurationSnapshot? =
            this@DefaultScriptConfigurationManager.getAppliedConfiguration(file)

        override fun suggestNewConfiguration(file: VirtualFile, newResult: ScriptConfigurationSnapshot) {
            suggestOrSaveConfiguration(file, newResult, false)
        }

        override fun saveNewConfiguration(file: VirtualFile, newResult: ScriptConfigurationSnapshot) {
            suggestOrSaveConfiguration(file, newResult, true)
        }
    }

    private fun suggestOrSaveConfiguration(
        file: VirtualFile,
        newResult: ScriptConfigurationSnapshot,
        skipNotification: Boolean
    ) {
        saveLock.withLock {
            debug(file) { "configuration received = $newResult" }

            setLoadedConfiguration(file, newResult)

            val newConfiguration = newResult.configuration
            if (newConfiguration == null) {
                saveReports(file, newResult.reports)
            } else {
                val old = getCachedConfigurationState(file)
                val oldConfiguration = old?.applied?.configuration
                if (oldConfiguration == newConfiguration) {
                    saveReports(file, newResult.reports)
                    file.removeScriptDependenciesNotificationPanel(project)
                } else {
                    val autoReload = skipNotification
                            || oldConfiguration == null
                            || KotlinScriptingSettings.getInstance(project).isAutoReloadEnabled
                            || ApplicationManager.getApplication().isUnitTestModeWithoutScriptLoadingNotification

                    if (autoReload) {
                        if (oldConfiguration != null) {
                            file.removeScriptDependenciesNotificationPanel(project)
                        }
                        saveReports(file, newResult.reports)
                        setAppliedConfiguration(file, newResult)
                    } else {
                        debug(file) {
                            "configuration changed, notification is shown: old = $oldConfiguration, new = $newConfiguration"
                        }

                        // restore reports for applied configuration in case of previous error
                        old?.applied?.reports?.let {
                            saveReports(file, it)
                        }

                        file.addScriptDependenciesNotificationPanel(
                            newConfiguration, project,
                            onClick = {
                                saveReports(file, newResult.reports)
                                file.removeScriptDependenciesNotificationPanel(project)
                                rootsIndexer.transaction {
                                    setAppliedConfiguration(file, newResult)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun saveReports(
        file: VirtualFile,
        newReports: List<ScriptDiagnostic>
    ) {
        val oldReports = IdeScriptReportSink.getReports(file)
        if (oldReports != newReports) {
            debug(file) { "new script reports = $newReports" }

            ServiceManager.getService(project, ScriptReportSink::class.java).attachReports(file, newReports)

            GlobalScope.launch(EDT(project)) {
                if (project.isDisposed) return@launch

                val ktFile = PsiManager.getInstance(project).findFile(file)
                if (ktFile != null) {
                    DaemonCodeAnalyzer.getInstance(project).restart(ktFile)
                }
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
        }
    }
}

object DefaultScriptConfigurationManagerExtensions {
    val LOADER: ExtensionPointName<ScriptConfigurationLoader> =
        ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.loader")

    val LISTENER: ExtensionPointName<ScriptChangeListener> =
        ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.listener")
}

val ScriptConfigurationManager.testingBackgroundExecutor
    get() = (this as DefaultScriptConfigurationManager).backgroundExecutor as TestingBackgroundExecutor
