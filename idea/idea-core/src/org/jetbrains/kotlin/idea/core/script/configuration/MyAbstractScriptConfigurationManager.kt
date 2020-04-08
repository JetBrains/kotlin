/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.core.script.KotlinScriptDependenciesClassFinder
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.loader.ScriptConfigurationLoader
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.isNonScript
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class MyAbstractScriptConfigurationManager(val project: Project) : ScriptConfigurationManager {
    abstract fun getOrLoadConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile? = null
    ): ScriptCompilationConfigurationWrapper?

    abstract fun getAllAppliedConfigurations(): Map<VirtualFile, ScriptCompilationConfigurationWrapper>

    abstract fun hasCachedConfiguration(file: KtFile): Boolean

    @TestOnly
    internal abstract fun clearCaches()

    abstract override val updater: ScriptConfigurationUpdater

    ///////////////////
    // Should be removed
    //

    override fun forceReloadConfiguration(file: VirtualFile, loader: ScriptConfigurationLoader): ScriptCompilationConfigurationWrapper? {
        // This seems to be Gradle only and should be named reloadOutOfProjectScriptConfiguration
        TODO("Not yet implemented")
    }

    override fun saveCompilationConfigurationAfterImport(files: List<Pair<VirtualFile, ScriptConfigurationSnapshot>>) {
        // This seems to be Gradle only and should be removed
        TODO("Not yet implemented")
    }

    // this should be named as getOrLoadConfiguration
    override fun getConfiguration(file: KtFile) = getOrLoadConfiguration(file.originalFile.virtualFile, file)

    // this should be named as hasCachedConfiguration
    override fun hasConfiguration(file: KtFile): Boolean = hasCachedConfiguration(file)

    ///////////////////
    // Adapters for deprecated API
    //

    override fun getScriptClasspath(file: KtFile): List<VirtualFile> =
        ScriptConfigurationManager.toVfsRoots(getConfiguration(file)?.dependenciesClassPath.orEmpty())

    ///////////////////
    // ScriptRootsCache

    override fun clearConfigurationCachesAndRehighlight() {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        // todo: clearCaches()
        clearClassRootsCaches()

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

                val value3 = ScriptClassRootsCache(project, getAllAppliedConfigurations())
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

    override fun getScriptSdk(file: VirtualFile): Sdk? = getActualClasspathRoots(file).getScriptSdk(file)

    override fun getFirstScriptsSdk(): Sdk? = classpathRoots.firstScriptSdk

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        getActualClasspathRoots(file).getScriptDependenciesClassFilesScope(file)

    override fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope = classpathRoots.allDependenciesClassFilesScope

    override fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope = classpathRoots.allDependenciesSourcesScope

    override fun getAllScriptsDependenciesClassFiles(): List<VirtualFile> = classpathRoots.allDependenciesClassFiles

    override fun getAllScriptDependenciesSources(): List<VirtualFile> = classpathRoots.allDependenciesSources
}