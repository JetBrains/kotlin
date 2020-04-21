/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
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

        abstract fun updateProjectRoots()

        var scopesListener: (() -> Unit)? = null

        companion object {
            val EPN: ExtensionPointName<Provider> =
                ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.scriptingSupportProvider")
        }
    }

    abstract fun clearCaches()
    abstract fun hasCachedConfiguration(file: KtFile): Boolean
    abstract fun getOrLoadConfiguration(virtualFile: VirtualFile, preloadedKtFile: KtFile? = null): ScriptCompilationConfigurationWrapper?

    abstract fun getScriptDependenciesClassFilesScope(file: VirtualFile): GlobalSearchScope

    abstract val updater: ScriptConfigurationUpdater

    abstract val firstScriptSdk: Sdk?

    abstract fun getScriptSdk(file: VirtualFile): Sdk?

    abstract fun addSearchScopeListener(scopesChanged: () -> Unit)
}