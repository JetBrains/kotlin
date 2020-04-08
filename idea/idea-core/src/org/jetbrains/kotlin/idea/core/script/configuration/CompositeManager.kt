/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.caches.project.getAllProjectSdks
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationCacheScope
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangesNotifier
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsIndexer
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.isNonScript
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper

interface ScriptingSupport {
    fun isRelated(file: VirtualFile): Boolean

    fun clearCaches()
    fun hasCachedConfiguration(file: KtFile): Boolean
    fun getOrLoadConfiguration(virtualFile: VirtualFile, preloadedKtFile: KtFile? = null): ScriptCompilationConfigurationWrapper?

    fun getAnyLoadedScript(): ScriptCompilationConfigurationWrapper?

    val updater: ScriptConfigurationUpdater

    fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope

    fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope
    fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope
    fun getAllScriptsDependenciesClassFiles(): List<VirtualFile>
    fun getAllScriptDependenciesSources(): List<VirtualFile>
}

class CompositeManager(val project: Project) : ScriptConfigurationManager {
    private val notifier = ScriptChangesNotifier(project, updater)

    private val managers = mutableListOf<ScriptingSupport>()

    private fun getRelatedManager(file: VirtualFile): ScriptingSupport = managers.first { it.isRelated(file) }
    private fun getRelatedManager(file: KtFile): ScriptingSupport = getRelatedManager(file.originalFile.virtualFile)

    private fun getOrLoadConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile? = null
    ): ScriptCompilationConfigurationWrapper? =
        getRelatedManager(virtualFile).getOrLoadConfiguration(virtualFile, preloadedKtFile)

    override fun getConfiguration(file: KtFile) = getOrLoadConfiguration(file.originalFile.virtualFile, file)

    override fun hasConfiguration(file: KtFile): Boolean =
        getRelatedManager(file).hasCachedConfiguration(file)

    override val updater: ScriptConfigurationUpdater
        get() = object : ScriptConfigurationUpdater {
            override fun ensureUpToDatedConfigurationSuggested(file: KtFile) =
                getRelatedManager(file).updater.ensureUpToDatedConfigurationSuggested(file)

            override fun ensureConfigurationUpToDate(files: List<KtFile>): Boolean =
                files.groupBy { getRelatedManager(it) }.all { (manager, files) ->
                    manager.updater.ensureConfigurationUpToDate(files)
                }

            override fun suggestToUpdateConfigurationIfOutOfDate(file: KtFile) =
                getRelatedManager(file).updater.suggestToUpdateConfigurationIfOutOfDate(file)
        }

    override fun getScriptSdk(file: VirtualFile): Sdk? =
        getScriptSdk(getOrLoadConfiguration(file))

    // todo: cache
    private fun getScriptSdk(compilationConfiguration: ScriptCompilationConfigurationWrapper?): Sdk? {
        // workaround for mismatched gradle wrapper and plugin version
        val javaHome = try {
            compilationConfiguration?.javaHome?.let { VfsUtil.findFileByIoFile(it, true) }
        } catch (e: Throwable) {
            null
        } ?: return null

        return getAllProjectSdks().find { it.homeDirectory == javaHome }
    }

    override fun getFirstScriptsSdk(): Sdk? {
        managers.forEach {
            val anyLoadedScript = it.getAnyLoadedScript()
            if (anyLoadedScript != null) {
                return getScriptSdk(anyLoadedScript)
            }
        }

        return null
    }

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        getRelatedManager(file).getScriptDependenciesClassFilesScope(file)

    override fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope =
        GlobalSearchScope.union(managers.map { it.getAllScriptsDependenciesClassFilesScope() })

    override fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope =
        GlobalSearchScope.union(managers.map { it.getAllScriptDependenciesSourcesScope() })

    override fun getAllScriptsDependenciesClassFiles(): List<VirtualFile> =
        managers.flatMap { it.getAllScriptsDependenciesClassFiles() }

    override fun getAllScriptDependenciesSources(): List<VirtualFile> =
        managers.flatMap { it.getAllScriptDependenciesSources() }

    ///////////////////
    // Should be removed
    //

    override fun forceReloadConfiguration(file: VirtualFile, loader: ScriptConfigurationLoader): ScriptCompilationConfigurationWrapper? {
        // This seems to be Gradle only and should be named reloadOutOfProjectScriptConfiguration
        TODO("Not yet implemented")
    }

    ///////////////////
    // Adapters for deprecated API
    //

    @Deprecated("Use getScriptClasspath(KtFile) instead")
    override fun getScriptClasspath(file: VirtualFile): List<VirtualFile> {
        val ktFile = project.getKtFile(file) ?: return emptyList()
        return getScriptClasspath(ktFile)
    }

    override fun getScriptClasspath(file: KtFile): List<VirtualFile> =
        ScriptConfigurationManager.toVfsRoots(getConfiguration(file)?.dependenciesClassPath.orEmpty())

    ///////////////////
    // ScriptRootsCache

    private fun clearCaches() {
        managers.forEach {
            it.clearCaches()
        }
    }

    override fun clearConfigurationCachesAndRehighlight() {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        clearCaches()

        if (project.isOpen) {
            val openedScripts = FileEditorManager.getInstance(project).openFiles.filterNot { it.isNonScript() }
            updateHighlighting(openedScripts)
        }
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
}