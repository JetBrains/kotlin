/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import org.jetbrains.kotlin.idea.core.script.KotlinScriptDependenciesClassFinder
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptConfigurationUpdater
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class ScriptingSupport {
    abstract class Provider {
        abstract val all: Collection<ScriptingSupport>

        abstract fun getSupport(file: VirtualFile): ScriptingSupport?

        companion object {
            val EPN: ExtensionPointName<Provider> =
                ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.scriptingSupportProvider")
        }
    }

    abstract fun clearCaches()
    abstract fun hasCachedConfiguration(file: KtFile): Boolean
    abstract fun isConfigurationLoadingInProgress(file: KtFile): Boolean
    abstract fun getOrLoadConfiguration(virtualFile: VirtualFile, preloadedKtFile: KtFile? = null): ScriptCompilationConfigurationWrapper?

    abstract val updater: ScriptConfigurationUpdater

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
                value3.saveClassRootsToStorage()
                _classpathRoots = value3
                return value3
            }
        }

    protected abstract fun recreateRootsCache(): ScriptClassRootsCache

    fun clearClassRootsCaches(project: Project) {
        debug { "class roots caches cleared" }

        classpathRootsLock.withLock {
            _classpathRoots = null
        }

        val kotlinScriptDependenciesClassFinder =
            Extensions.getArea(project)
                .getExtensionPoint(PsiElementFinder.EP_NAME).extensions
                .filterIsInstance<KotlinScriptDependenciesClassFinder>()
                .single()

        kotlinScriptDependenciesClassFinder.clearCache()

        ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
    }
}