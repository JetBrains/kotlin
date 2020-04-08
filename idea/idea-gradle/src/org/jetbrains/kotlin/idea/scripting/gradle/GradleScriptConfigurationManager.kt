/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.idea.core.script.configuration.AbstractScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoadingContext
import org.jetbrains.kotlin.idea.core.script.configuration.utils.*
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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


class GradleScriptConfigurationManagerProvider : SpecialScriptConfigurationManagerProvider {
    override fun getSpecialScriptConfigurationManager(ktFile: KtFile): AbstractScriptConfigurationManager? {
        if (isGradleKotlinScript(ktFile.originalFile.virtualFile)) {
            val project = ktFile.project
            val gradleVersion = getGradleVersion(project)
            if (gradleVersion != null && kotlinDslScriptsModelImportSupported(gradleVersion)) {
                return GradleScriptConfigurationManager(project)
            }
            return GradleScriptConfigurationManagerTroughScriptingAPI(project)
        }
        return null
    }
}

open class GradleScriptConfigurationManager(project: Project) : AbstractScriptConfigurationManager(project) {

    private val saveLock = ReentrantLock()

    override fun reloadOutOfDateConfiguration(
        file: KtFile,
        virtualFile: VirtualFile,
        definition: ScriptDefinition,
        forceSync: Boolean, /* test only */
        postponeLoading: Boolean
    ) {
        if (forceSync) {
            loadDependenciesIfNeeded(file, definition)
            return
        }

        reloadConfiguration(file, virtualFile, definition, forceSync, postponeLoading)
    }

    open fun reloadConfiguration(
        file: KtFile,
        vFile: VirtualFile,
        definition: ScriptDefinition,
        forceSync: Boolean,
        postponeLoading: Boolean
    ) {
        if (postponeLoading) {
            showNotificationForProjectImport(project)
        } else {
            runPartialGradleImport(project)
        }
    }

    override fun forceReloadConfiguration(file: KtFile, loader: ScriptConfigurationLoader) {
        if (!ScriptDefinitionsManager.getInstance(project).isReady()) return
        val scriptDefinition = file.findScriptDefinition() ?: return

        loadDependenciesIfNeeded(file, scriptDefinition)
    }

    /**
     * Save configurations into cache.
     * Start indexing for new class/source roots.
     * Re-highlight opened scripts with changed configuration.
     */
    override fun saveCompilationConfigurationAfterImport(files: List<Pair<VirtualFile, ScriptConfigurationSnapshot>>) {
        rootsIndexer.transaction {
            for ((file, result) in files) {
                saveConfiguration(file, result)
            }
        }
    }

    private fun loadDependenciesIfNeeded(
        ktFile: KtFile,
        scriptDefinition: ScriptDefinition
    ) {
        if (useScriptConfigurationFromImportOnly()) {
            // do nothing, project import notification will be already showed
            // and configuration for gradle build scripts will be saved at the end of import
            return
        }

        val vFile = ktFile.originalFile.virtualFile
        if (!isInAffectedGradleProjectFiles(ktFile.project, vFile.path)) {
            ScriptConfigurationManager.markFileWithManualConfigurationLoading(vFile)
            return
        }

        ScriptConfigurationManager.clearManualConfigurationLoadingIfNeeded(vFile)

        // Gradle read files from FS
        GlobalScope.launch(EDT(project)) {
            runWriteAction {
                FileDocumentManager.getInstance().saveAllDocuments()
            }
        }

        loadDependencies(ktFile, scriptDefinition)
    }

    open fun loadDependencies(ktFile: KtFile, scriptDefinition: ScriptDefinition) {
        runPartialGradleImport(project)
    }

    override val loadingContext = object : ScriptConfigurationLoadingContext {
        override fun getCachedConfiguration(file: VirtualFile): ScriptConfigurationSnapshot? {
            return getAppliedConfiguration(file)
        }

        override fun suggestNewConfiguration(file: VirtualFile, newResult: ScriptConfigurationSnapshot) {
            throw UnsupportedOperationException("Shouldn't be called for Gradle Scripts")
        }

        override fun saveNewConfiguration(file: VirtualFile, newResult: ScriptConfigurationSnapshot) {
            saveConfiguration(file, newResult)
        }
    }

    protected fun saveConfiguration(
        file: VirtualFile,
        newResult: ScriptConfigurationSnapshot
    ) {
        saveLock.withLock {
            debug(file) { "configuration received = $newResult" }

            setLoadedConfiguration(file, newResult)

            saveReports(file, newResult.reports)

            val newConfiguration = newResult.configuration
            if (newConfiguration != null) {
                val old = getCachedConfigurationState(file)
                val oldConfiguration = old?.applied?.configuration
                if (oldConfiguration == null || !areSimilar(oldConfiguration, newConfiguration)) {
                    setAppliedConfiguration(file, newResult)
                }
            }
        }
    }
}
