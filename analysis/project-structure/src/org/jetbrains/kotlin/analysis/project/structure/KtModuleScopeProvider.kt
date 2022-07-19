/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

public abstract class KtModuleScopeProvider {
    /**
     * Get a scope of binaries on which current source module depends.
     * Should be equivalent to
     *
     * ```
     * GlobalSearchScope.union(
     *    sourceModule.allDependenciesOfType<KtBinaryModule>()
     *        .map { it.contentScope }
     * )
     * ```
     * For the IDE there can be more optimal implementations.
     *
     * See [KtModuleScopeProviderImpl] a correct but non-optimal implementation.
     */
    public abstract fun getModuleLibrariesScope(sourceModule: KtSourceModule): GlobalSearchScope
}

public class KtModuleScopeProviderImpl : KtModuleScopeProvider() {
    override fun getModuleLibrariesScope(sourceModule: KtSourceModule): GlobalSearchScope {
        val scopes = sourceModule.allDirectDependenciesOfType<KtBinaryModule>()
            .map { it.contentScope }
            .toList()
        if (scopes.isEmpty()) return GlobalSearchScope.EMPTY_SCOPE
        return GlobalSearchScope.union(scopes)
    }
}

public val Project.moduleScopeProvider: KtModuleScopeProvider
    get() = this.getService(KtModuleScopeProvider::class.java)
