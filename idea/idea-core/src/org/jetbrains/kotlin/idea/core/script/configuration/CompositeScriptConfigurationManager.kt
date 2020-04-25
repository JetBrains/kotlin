/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.ProjectTopics
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.core.script.KotlinScriptDependenciesClassFinder
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangesNotifier
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsIndexer
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.isNonScript
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CompositeScriptConfigurationManager(val project: Project) : ScriptConfigurationManager {
    @Suppress("unused")
    private val notifier = ScriptChangesNotifier(project)

    private val plugins = ScriptingSupport.Provider.EPN.getPoint(project).extensionList

    val default = DefaultScriptingSupport(this)

    private fun getOrLoadConfiguration(
        virtualFile: VirtualFile,
        preloadedKtFile: KtFile? = null
    ): ScriptCompilationConfigurationWrapper? =
        classpathRoots.getScriptConfiguration(virtualFile)
            ?: default.getOrLoadConfiguration(virtualFile, preloadedKtFile)

    override fun getConfiguration(file: KtFile) =
        getOrLoadConfiguration(file.originalFile.virtualFile, file)

    override fun hasConfiguration(file: KtFile): Boolean =
        classpathRoots.contains(file.originalFile.virtualFile)

    override fun isConfigurationLoadingInProgress(file: KtFile): Boolean =
        plugins.any { it.isConfigurationLoadingInProgress(file) }

    val rootsUpdater = ScriptClassRootsIndexer(project, this)

    private val classpathRootsLock = ReentrantLock()

    @Volatile
    private var _classpathRoots: ScriptClassRootsCache? = null
    val classpathRoots: ScriptClassRootsCache
        get() {
            val value1 = _classpathRoots
            if (value1 != null) return value1

            classpathRootsLock.withLock {
                val value2 = _classpathRoots
                if (value2 != null) return value2

                val value3 = recreateRootsCache()
                _classpathRoots = value3
                return value3
            }
        }

    private fun recreateRootsCache(): ScriptClassRootsCache {
        val builder = ScriptClassRootsCache.Builder(project)
        default.collectConfigurations(builder)
        plugins.forEach { it.collectConfigurations(builder) }
        return builder.build()
    }

    fun collectRootsAndCheckNew(): Boolean {
        classpathRootsLock.withLock {
            val old = _classpathRoots
            val new = recreateRootsCache()
            _classpathRoots = new

            val kotlinScriptDependenciesClassFinder =
                Extensions.getArea(project)
                    .getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                    .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                    .single()

            kotlinScriptDependenciesClassFinder.clearCache()

            ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

            return old == null || new.hasNewRoots(old)
        }
    }

    fun clearClassRootsCaches() {
        debug { "class roots caches cleared" }

        classpathRootsLock.withLock {
            _classpathRoots = null
        }
    }

    init {
        val connection = project.messageBus.connect(project)
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                clearClassRootsCaches()
            }
        })
    }

    /**
     * Returns script classpath roots
     * Loads script configuration if classpath roots don't contain [file] yet
     */
    private fun getActualClasspathRoots(file: VirtualFile): ScriptClassRootsCache {
        val classpathRoots = classpathRoots
        if (classpathRoots.contains(file)) {
            return classpathRoots
        }

        getOrLoadConfiguration(file)

        return this.classpathRoots
    }

    override fun getScriptSdk(file: VirtualFile): Sdk? =
        getActualClasspathRoots(file).getScriptSdk(file)

    override fun getFirstScriptsSdk(): Sdk? =
        classpathRoots.firstScriptSdk

    override fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope =
        classpathRoots.getScriptDependenciesClassFilesScope(file)

    override fun getAllScriptsDependenciesClassFilesScope(): GlobalSearchScope =
        classpathRoots.allDependenciesClassFilesScope

    override fun getAllScriptDependenciesSourcesScope(): GlobalSearchScope =
        classpathRoots.allDependenciesSourcesScope

    override fun getAllScriptsDependenciesClassFiles(): List<VirtualFile> =
        classpathRoots.allDependenciesClassFiles

    override fun getAllScriptDependenciesSources(): List<VirtualFile> =
        classpathRoots.allDependenciesSources

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

    private fun clearCaches() {
        clearClassRootsCaches()
        plugins.forEach { it.clearCaches() }
    }

    override fun clearConfigurationCachesAndRehighlight() {
        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

        clearCaches()

        ScriptingSupportHelper.updateHighlighting(project) {
            !it.isNonScript()
        }
    }
}