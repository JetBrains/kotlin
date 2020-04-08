/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.EditorNotifications
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.core.script.configuration.cache.*
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationFileAttributeCache
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangesNotifier
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.loader.DefaultScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptOutsiderFileConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.utils.*
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.isNonScript
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.script.experimental.api.ScriptDiagnostic

class DefaultScriptingSupport(project: Project) : DefaultScriptingSupportBase(project) {
    private val backgroundExecutor: BackgroundExecutor =
        if (ApplicationManager.getApplication().isUnitTestMode) TestingBackgroundExecutor(rootsIndexer)
        else DefaultBackgroundExecutor(project, rootsIndexer)

    private val outsiderLoader = ScriptOutsiderFileConfigurationLoader(project)
    private val fileAttributeCache = ScriptConfigurationFileAttributeCache(project)
    private val defaultLoader = DefaultScriptConfigurationLoader(project)
    private val loaders = listOf(outsiderLoader, fileAttributeCache, defaultLoader)

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
     * [clearConfigurationCachesAndRehighlight] will be called when [ScriptDefinitionsManager] will be ready
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
            if (forceSync) {
                loaders.firstOrNull { it.loadDependencies(isFirstLoad, file, scriptDefinition, loadingContext) }
            } else {
                backgroundExecutor.ensureScheduled(virtualFile) {
                    val cached = getCachedConfigurationState(virtualFile)

                    val applied = cached?.applied
                    if (applied != null && applied.inputs.isUpToDate(project, virtualFile)) {
                        // in case user reverted to applied configuration
                        suggestOrSaveConfiguration(virtualFile, applied, true)
                    } else if (cached == null || !cached.isUpToDate(project, virtualFile)) {
                        // don't start loading if nothing was changed
                        // (in case we checking for up-to-date and loading concurrently)
                        val actualIsFirstLoad = cached == null
                        async.firstOrNull { it.loadDependencies(actualIsFirstLoad, file, scriptDefinition, loadingContext) }
                    }
                }
            }
        }
    }

    override fun forceReloadConfiguration(file: KtFile, loader: ScriptConfigurationLoader) {
        val virtualFile = file.originalFile.virtualFile ?: return

        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return
        val scriptDefinition = file.findScriptDefinition() ?: return

        if (!loader.shouldRunInBackground(scriptDefinition)) {
            loader.loadDependencies(false, file, scriptDefinition, loadingContext)
        } else {
            backgroundExecutor.ensureScheduled(virtualFile) {
                loader.loadDependencies(false, file, scriptDefinition, loadingContext)
            }
        }
    }

    private val loadingContext = object : ScriptConfigurationLoadingContext {
        override fun getCachedConfiguration(file: VirtualFile): ScriptConfigurationSnapshot? =
            getAppliedConfiguration(file)

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
                if (oldConfiguration != null && areSimilar(oldConfiguration, newConfiguration)) {
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

abstract class DefaultScriptingSupportBase(val project: Project): ScriptingSupport {
    protected val rootsIndexer = ScriptClassRootsIndexer(project)

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
     * @param loadEvenWillNotBeApplied may should be set to false only on requests from particular editor, when
     * user can see potential notification and accept new configuration. In other cases this should be `false` since
     * loaded configuration will be just leaved in hidden user notification and cannot be used in any way.
     * @param forceSync should be used in tests only
     * @param isPostponedLoad is used to postspone loading: show a notification for out of date script and start loading when user request
     */
    protected abstract fun reloadOutOfDateConfiguration(
        file: KtFile,
        isFirstLoad: Boolean = getAppliedConfiguration(file.originalFile.virtualFile) == null,
        loadEvenWillNotBeApplied: Boolean = false,
        forceSync: Boolean = false
    )

    /**
     * Will be called on user action
     * Load configuration event it is already cached or inputs are up-to-date
     *
     * @param loader is used to load configuration. Other loaders aren't taken into account.
     */
    protected abstract fun forceReloadConfiguration(
        file: KtFile,
        loader: ScriptConfigurationLoader
    )

    fun getCachedConfigurationState(file: VirtualFile?): ScriptConfigurationState? {
        if (file == null) return null
        return cache[file]
    }

    fun getAppliedConfiguration(file: VirtualFile?): ScriptConfigurationSnapshot? =
        getCachedConfigurationState(file)?.applied

    override fun isRelated(file: VirtualFile): Boolean = true

    override fun hasCachedConfiguration(file: KtFile): Boolean =
        getAppliedConfiguration(file.originalFile.virtualFile) != null

    override fun getAnyLoadedScript(): ScriptCompilationConfigurationWrapper? =
        cache.getAnyLoadedScript()

    override fun getOrLoadConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile?
    ): ScriptCompilationConfigurationWrapper? {
        val cached = getAppliedConfiguration(virtualFile)
        if (cached != null) return cached.configuration

        val ktFile = project.getKtFile(virtualFile, preloadedKtFile) ?: return null
        rootsIndexer.transaction {
            reloadOutOfDateConfiguration(ktFile, isFirstLoad = true)
        }

        return getAppliedConfiguration(virtualFile)?.configuration
    }

    override val updater: ScriptConfigurationUpdater = object : ScriptConfigurationUpdater {
        override fun ensureUpToDatedConfigurationSuggested(file: KtFile) {
            reloadIfOutOfDate(listOf(file), loadEvenWillNotBeApplied = true, isPostponedLoad = false)
        }

        override fun ensureConfigurationUpToDate(files: List<KtFile>): Boolean {
            return reloadIfOutOfDate(files, loadEvenWillNotBeApplied = false, isPostponedLoad = false)
        }

        override fun postponeConfigurationReload(scope: ScriptConfigurationCacheScope) {
            cache.markOutOfDate(scope)
        }

        override fun suggestToUpdateConfigurationIfOutOfDate(file: KtFile) {
            reloadIfOutOfDate(listOf(file), loadEvenWillNotBeApplied = true, isPostponedLoad = true)
        }
    }

    private fun reloadIfOutOfDate(files: List<KtFile>, loadEvenWillNotBeApplied: Boolean, isPostponedLoad: Boolean): Boolean {
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return false

        var upToDate = true
        rootsIndexer.transaction {
            files.forEach { file ->
                val virtualFile = file.originalFile.virtualFile
                if (virtualFile != null) {
                    val state = cache[virtualFile]
                    if (state == null || !state.isUpToDate(project, virtualFile, file)) {
                        upToDate = false
                        reloadOutOfDateConfiguration(
                            file,
                            isFirstLoad = state == null,
                            loadEvenWillNotBeApplied = loadEvenWillNotBeApplied
                        )
                    }
                }
            }
        }

        return upToDate
    }

    @TestOnly
    internal fun updateScriptDependenciesSynchronously(file: PsiFile) {
        file.findScriptDefinition() ?: return

        file as? KtFile ?: error("PsiFile $file should be a KtFile, otherwise script dependencies cannot be loaded")

        val virtualFile = file.virtualFile
        if (cache[virtualFile]?.isUpToDate(project, virtualFile, file) == true) return

        rootsIndexer.transaction {
            reloadOutOfDateConfiguration(file, forceSync = true, loadEvenWillNotBeApplied = true)
        }
    }

    protected open fun setAppliedConfiguration(
        file: VirtualFile,
        newConfigurationSnapshot: ScriptConfigurationSnapshot?
    ) {
        rootsIndexer.checkInTransaction()
        val newConfiguration = newConfigurationSnapshot?.configuration
        debug(file) { "configuration changed = $newConfiguration" }

        if (newConfiguration != null) {
            if (hasNotCachedRoots(newConfiguration)) {
                rootsIndexer.markNewRoot(file, newConfiguration)
            }

            cache.setApplied(file, newConfigurationSnapshot)

            clearClassRootsCaches()
        }

        updateHighlighting(listOf(file))
    }

    protected fun setLoadedConfiguration(
        file: VirtualFile,
        configurationSnapshot: ScriptConfigurationSnapshot
    ) {
        cache.setLoaded(file, configurationSnapshot)
    }

    private fun hasNotCachedRoots(configuration: ScriptCompilationConfigurationWrapper): Boolean {
        return classpathRoots.hasNotCachedRoots(configuration)
    }

    init {
        val connection = project.messageBus.connect(project)
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                clearClassRootsCaches()
            }
        })
    }

    override fun clearCaches() {
        cache.clear()
    }

    private fun updateHighlighting(files: List<VirtualFile>) {
        if (files.isEmpty()) return

        GlobalScope.launch(EDT(project)) {
            if (project.isDisposed) return@launch

            val openFiles = FileEditorManager.getInstance(project).openFiles
            val openScripts = files.filter { it.isValid && openFiles.contains(it) }

            openScripts.forEach {
                PsiManager.getInstance(project).findFile(it)?.let { psiFile ->
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }
    }

    ///////////////////
    // ScriptRootsCache

    private val classpathRootsLock = ReentrantLock()

    @Volatile
    private var _classpathRoots: ScriptClassRootsCache? = null
    private val classpathRoots: ScriptClassRootsCache
        get() {
            val value1 = _classpathRoots
            if (value1 != null) return value1

            classpathRootsLock.withLock {
                val value2 = _classpathRoots
                if (value2 != null) return value2

                val value3 = ScriptClassRootsCache(project, cache.allApplied())
                _classpathRoots = value3
                return value3
            }
        }

    private fun clearClassRootsCaches() {
        debug { "class roots caches cleared" }

        classpathRootsLock.withLock {
            _classpathRoots = null
        }

        val kotlinScriptDependenciesClassFinder =
            Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()

        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
    }

    /**
     * Returns script classpath roots
     * Loads script configuration if classpath roots don't contain [file] yet
     */
    private fun getActualClasspathRoots(file: VirtualFile): ScriptClassRootsCache {
        if (classpathRoots.contains(file)) {
            return classpathRoots
        }

        getOrLoadConfiguration(file)

        return classpathRoots
    }

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        getActualClasspathRoots(file).getScriptDependenciesClassFilesScope(file)

    override fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope = classpathRoots.allDependenciesClassFilesScope

    override fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope = classpathRoots.allDependenciesSourcesScope

    override fun getAllScriptsDependenciesClassFiles(): List<VirtualFile> = classpathRoots.allDependenciesClassFiles

    override fun getAllScriptDependenciesSources(): List<VirtualFile> = classpathRoots.allDependenciesSources
}