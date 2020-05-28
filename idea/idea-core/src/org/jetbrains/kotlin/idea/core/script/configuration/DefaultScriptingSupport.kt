/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptConfigurationManagerExtensions.LOADER
import org.jetbrains.kotlin.idea.core.script.configuration.cache.*
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptOutsiderFileConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsBuilder
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsStorage
import org.jetbrains.kotlin.idea.core.script.configuration.utils.*
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.script.experimental.api.ScriptDiagnostic

/**
 * Standard implementation of scripts configuration loading and caching.
 *
 * ## Loading initiation
 *
 * [getOrLoadConfiguration] will be called when we need to show or analyze some script file.
 *
 * As described in [DefaultScriptingSupportBase], configuration may be loaded from [cache]
 * or [reloadOutOfDateConfiguration] will be called on [cache] miss.
 *
 * There are 2 tiers [cache]: memory and FS. For now FS cache implemented by [ScriptConfigurationLoader]
 * because we are not storing classpath roots yet. As a workaround cache.all() will return only memory
 * cached configurations.  So, for now we are indexing roots that loaded from FS with
 * default [reloadOutOfDateConfiguration] mechanics.
 *
 * Also, [ensureLoadedFromCache] may be called from [UnusedSymbolInspection]
 * to ensure that configuration of all scripts containing some symbol are up-to-date or try load it in sync.
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
class DefaultScriptingSupport(manager: CompositeScriptConfigurationManager) : DefaultScriptingSupportBase(manager) {
    // TODO public for tests
    val backgroundExecutor: BackgroundExecutor =
        if (ApplicationManager.getApplication().isUnitTestMode) TestingBackgroundExecutor(manager)
        else DefaultBackgroundExecutor(project, manager)

    private val outsiderLoader = ScriptOutsiderFileConfigurationLoader(project)
    private val fileAttributeCache = ScriptConfigurationFileAttributeCache(project)
    private val defaultLoader = DefaultScriptConfigurationLoader(project)
    private val loaders: List<ScriptConfigurationLoader>
        get() = mutableListOf<ScriptConfigurationLoader>().apply {
            add(outsiderLoader)
            add(fileAttributeCache)
            addAll(LOADER.getPoint(project).extensionList)
            add(defaultLoader)
        }

    private val saveLock = ReentrantLock()

    override fun createCache() = object : ScriptConfigurationMemoryCache(project) {
        override fun setLoaded(file: VirtualFile, configurationSnapshot: ScriptConfigurationSnapshot) {
            super.setLoaded(file, configurationSnapshot)
            fileAttributeCache.save(file, configurationSnapshot)
        }
    }

    /**
     * Will be called on [cache] miss to initiate loading of [file]'s script configuration.
     *
     * ## Concurrency
     *
     * Each files may be in on of the states described below:
     * - scriptDefinition is not ready. `ScriptDefinitionsManager.getInstance(project).isReady() == false`.
     * [updateScriptDefinitionsReferences] will be called when [ScriptDefinitionsManager] will be ready
     * which will call [reloadOutOfDateConfiguration] for opened editors.
     * - unknown. When [isFirstLoad] true (`cache[file] == null`).
     * - up-to-date. `cache[file]?.upToDate == true`.
     * - invalid, in queue. `cache[file]?.upToDate == false && file in backgroundExecutor`.
     * - invalid, loading. `cache[file]?.upToDate == false && file !in backgroundExecutor`.
     * - invalid, waiting for apply. `cache[file]?.upToDate == false && file !in backgroundExecutor` and has notification panel?
     * - invalid, waiting for update. `cache[file]?.upToDate == false` and has notification panel
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
     * - `invalid, waiting for update`:
     *   Loading wasn't started, only notification is shown
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
        forceSync: Boolean,
        isPostponedLoad: Boolean,
        fromCacheOnly: Boolean
    ): Boolean {
        val virtualFile = file.originalFile.virtualFile ?: return false

        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return false
        val scriptDefinition = file.findScriptDefinition() ?: return false

        val (async, sync) = loaders.partition { it.shouldRunInBackground(scriptDefinition) }

        val syncLoader = sync.firstOrNull { it.loadDependencies(isFirstLoad, file, scriptDefinition, loadingContext) }
        if (syncLoader == null) {
            if (!fromCacheOnly) {
                if (forceSync) {
                    loaders.firstOrNull { it.loadDependencies(isFirstLoad, file, scriptDefinition, loadingContext) }
                } else {
                    val autoReloadEnabled = KotlinScriptingSettings.getInstance(project).autoReloadConfigurations(scriptDefinition)
                    val postponeLoading = isPostponedLoad && !autoReloadEnabled

                    if (postponeLoading) {
                        LoadScriptConfigurationNotificationFactory.showNotification(virtualFile, project) {
                            runAsyncLoaders(file, virtualFile, scriptDefinition, async, isLoadingPostponed = true)
                        }
                    } else {
                        runAsyncLoaders(file, virtualFile, scriptDefinition, async, isLoadingPostponed = false)
                    }
                }
            }

            return false
        } else {
            return true
        }
    }

    private fun runAsyncLoaders(
        file: KtFile,
        virtualFile: VirtualFile,
        scriptDefinition: ScriptDefinition,
        loaders: List<ScriptConfigurationLoader>,
        isLoadingPostponed: Boolean
    ) {
        backgroundExecutor.ensureScheduled(virtualFile) {
            val cached = getCachedConfigurationState(virtualFile)

            val applied = cached?.applied
            if (applied != null && applied.inputs.isUpToDate(project, virtualFile)) {
                // in case user reverted to applied configuration
                val skipNotification = isLoadingPostponed || KotlinScriptingSettings.getInstance(project).autoReloadConfigurations(scriptDefinition)
                suggestOrSaveConfiguration(virtualFile, applied, skipNotification)
            } else if (cached == null || !cached.isUpToDate(project, virtualFile)) {
                // don't start loading if nothing was changed
                // (in case we checking for up-to-date and loading concurrently)
                val actualIsFirstLoad = cached == null
                loaders.firstOrNull { it.loadDependencies(actualIsFirstLoad, file, scriptDefinition, loadingContext) }
            }
        }
    }

    fun forceReloadConfiguration(file: KtFile, loader: ScriptConfigurationLoader): ScriptCompilationConfigurationWrapper? {
        val virtualFile = file.originalFile.virtualFile ?: return null

        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return null
        val scriptDefinition = file.findScriptDefinition() ?: return null

        manager.updater.update {
            if (!loader.shouldRunInBackground(scriptDefinition)) {
                loader.loadDependencies(false, file, scriptDefinition, loadingContext)
            } else {
                backgroundExecutor.ensureScheduled(virtualFile) {
                    loader.loadDependencies(false, file, scriptDefinition, loadingContext)
                }
            }
        }

        return getAppliedConfiguration(virtualFile)?.configuration
    }

    private val loadingContext = object : ScriptConfigurationLoadingContext {
        /**
         * Used from [ScriptOutsiderFileConfigurationLoader] only.
         */
        override fun getCachedConfiguration(file: VirtualFile): ScriptConfigurationSnapshot? =
            getAppliedConfiguration(file) ?: getFromGlobalCache(file)

        private fun getFromGlobalCache(file: VirtualFile): ScriptConfigurationSnapshot? {
            val info = manager.getLightScriptInfo(file.path) ?: return null
            return ScriptConfigurationSnapshot(CachedConfigurationInputs.UpToDate, listOf(), info.buildConfiguration())
        }

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

            LoadScriptConfigurationNotificationFactory.hideNotification(file, project)

            val newConfiguration = newResult.configuration
            if (newConfiguration == null) {
                saveReports(file, newResult.reports)
            } else {
                val old = getCachedConfigurationState(file)
                val oldConfiguration = old?.applied?.configuration
                if (oldConfiguration != null && areSimilar(oldConfiguration, newConfiguration)) {
                    saveReports(file, newResult.reports)
                    file.removeScriptDependenciesNotificationPanel(project)
                } else {
                    val autoReload = skipNotification
                            || oldConfiguration == null
                            || ApplicationManager.getApplication().isUnitTestModeWithoutScriptLoadingNotification

                    if (autoReload) {
                        if (oldConfiguration != null) {
                            file.removeScriptDependenciesNotificationPanel(project)
                        }
                        saveReports(file, newResult.reports)
                        setAppliedConfiguration(file, newResult, syncUpdate = true)
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
                                manager.updater.update {
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

    companion object {
        fun getInstance(project: Project) =
            (ScriptConfigurationManager.getInstance(project) as CompositeScriptConfigurationManager).default
    }
}

/**
 * Abstraction for [DefaultScriptingSupportBase] based [cache] and [reloadOutOfDateConfiguration].
 * Among this two methods concrete implementation should provide script changes listening.
 *
 * Basically all requests routed to [cache]. If there is no entry in [cache] or it is considered out-of-date,
 * then [reloadOutOfDateConfiguration] will be called, which, in turn, should call [setAppliedConfiguration]
 * immediately or in some future  (e.g. after user will click "apply context" or/and configuration will
 * be calculated by some background thread).
 *
 * [ScriptClassRootsCache] will be calculated based on [cache]d configurations.
 * Every change in [cache] will invalidate [ScriptClassRootsCache] cache.
 */
abstract class DefaultScriptingSupportBase(val manager: CompositeScriptConfigurationManager) {
    val project: Project
        get() = manager.project

    @Suppress("LeakingThis")
    protected val cache: ScriptConfigurationCache = createCache()

    protected abstract fun createCache(): ScriptConfigurationCache

    /**
     * Will be called on [cache] miss or when [file] is changed.
     * Implementation should initiate loading of [file]'s script configuration and call [setAppliedConfiguration]
     * immediately or in some future
     * (e.g. after user will click "apply context" or/and configuration will be calculated by some background thread).
     *
     * @param isFirstLoad may be set explicitly for optimization reasons (to avoid expensive fs cache access)
     * @param forceSync should be used in tests only
     * @param isPostponedLoad is used to postspone loading: show a notification for out of date script and start loading when user request
     * @param fromCacheOnly load only when builtin fast synchronous loaders are available
     * @return true, if configuration loaded in sync
     */
    protected abstract fun reloadOutOfDateConfiguration(
        file: KtFile,
        isFirstLoad: Boolean = getAppliedConfiguration(file.originalFile.virtualFile) == null,
        forceSync: Boolean = false,
        isPostponedLoad: Boolean = false,
        fromCacheOnly: Boolean = false
    ): Boolean

    fun getCachedConfigurationState(file: VirtualFile?): ScriptConfigurationState? {
        if (file == null) return null
        return cache[file]
    }

    fun getAppliedConfiguration(file: VirtualFile?): ScriptConfigurationSnapshot? =
        getCachedConfigurationState(file)?.applied

    private fun hasCachedConfiguration(file: KtFile): Boolean =
        getAppliedConfiguration(file.originalFile.virtualFile) != null

    fun isConfigurationLoadingInProgress(file: KtFile): Boolean {
        return !hasCachedConfiguration(file) && !ScriptConfigurationManager.isManualConfigurationLoading(file.originalFile.virtualFile)
    }

    fun getOrLoadConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile?
    ): ScriptCompilationConfigurationWrapper? {
        val cached = getAppliedConfiguration(virtualFile)
        if (cached != null) return cached.configuration

        val ktFile = project.getKtFile(virtualFile, preloadedKtFile) ?: return null
        manager.updater.update {
            reloadOutOfDateConfiguration(ktFile, isFirstLoad = true)
        }

        return getAppliedConfiguration(virtualFile)?.configuration
    }

    /**
     * Load new configuration and suggest to apply it (only if it is changed)
     */
    fun ensureUpToDatedConfigurationSuggested(file: KtFile) {
        reloadIfOutOfDate(file)
    }

    /**
     * Show notification about changed script configuration with action to start loading it
     */
    fun suggestToUpdateConfigurationIfOutOfDate(file: KtFile) {
        reloadIfOutOfDate(file, isPostponedLoad = true)
    }

    private fun reloadIfOutOfDate(file: KtFile, isPostponedLoad: Boolean = false) {
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return

        manager.updater.update {
            val virtualFile = file.originalFile.virtualFile
            if (virtualFile != null) {
                val state = cache[virtualFile]
                if (state == null || !state.isUpToDate(project, virtualFile, file)) {
                    reloadOutOfDateConfiguration(
                        file,
                        isFirstLoad = state == null,
                        isPostponedLoad = isPostponedLoad
                    )
                }
            }
        }
    }

    /**
     * Ensure that any configuration for [files] is loaded from cache
     */
    fun ensureLoadedFromCache(files: List<KtFile>): Boolean {
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return false

        var allLoaded = true
        manager.updater.update {
            files.forEach { file ->
                val virtualFile = file.originalFile.virtualFile
                if (virtualFile != null) {
                    val state = cache[virtualFile]
                    if (state == null) {
                        if (!reloadOutOfDateConfiguration(
                                file,
                                isFirstLoad = true,
                                fromCacheOnly = true
                            )
                        ) {
                            allLoaded = false
                        }
                    }
                }
            }
        }

        return allLoaded
    }

    protected open fun setAppliedConfiguration(
        file: VirtualFile,
        newConfigurationSnapshot: ScriptConfigurationSnapshot?,
        syncUpdate: Boolean = false
    ) {
        manager.updater.checkInTransaction()
        val newConfiguration = newConfigurationSnapshot?.configuration
        debug(file) { "configuration changed = $newConfiguration" }

        if (newConfiguration != null) {
            cache.setApplied(file, newConfigurationSnapshot)
            manager.updater.invalidate(file, synchronous = syncUpdate)
        }
    }

    protected fun setLoadedConfiguration(
        file: VirtualFile,
        configurationSnapshot: ScriptConfigurationSnapshot
    ) {
        cache.setLoaded(file, configurationSnapshot)
    }

    @TestOnly
    internal fun updateScriptDependenciesSynchronously(file: PsiFile) {
        file.findScriptDefinition() ?: return

        file as? KtFile ?: error("PsiFile $file should be a KtFile, otherwise script dependencies cannot be loaded")

        val virtualFile = file.virtualFile
        if (cache[virtualFile]?.isUpToDate(project, virtualFile, file) == true) return

        manager.updater.update {
            reloadOutOfDateConfiguration(file, forceSync = true)
        }
    }

    fun updateScriptDefinitionsReferences() {
        cache.clear()
    }

    fun collectConfigurations(builder: ScriptClassRootsBuilder) {
        // todo: drop the hell below
        // keep this one only:
        // cache.allApplied().forEach { (vFile, configuration) -> builder.add(vFile, configuration) }

        // own builder for saving to storage
        val rootsStorage = ScriptClassRootsStorage.getInstance(project)
        val ownBuilder = ScriptClassRootsBuilder.fromStorage(project, rootsStorage)
        cache.allApplied().forEach { (vFile, configuration) -> ownBuilder.add(vFile, configuration) }
        ownBuilder.toStorage(rootsStorage)

        builder.add(ownBuilder)
    }
}

object DefaultScriptConfigurationManagerExtensions {
    val LOADER: ExtensionPointName<ScriptConfigurationLoader> =
        ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.loader")
}

val ScriptConfigurationManager.testingBackgroundExecutor
    get() = (this as CompositeScriptConfigurationManager).default
        .backgroundExecutor as TestingBackgroundExecutor