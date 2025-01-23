/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * A scope used for resolving files inside [useSiteModule].
 * This resulting scope is a union of [useSiteModule]'s content scope and content scopes of modules [useSiteModule] depends on.
 */
@KaImplementationDetail
public abstract class KaResolutionScope : GlobalSearchScope() {
    /**
     * Checks whether a [PsiElement] is contained in the resolution scope,
     * i.e., the containing file is present in the resolution scope.
     *
     * In contrast to [GlobalSearchScope.contains],
     * this method provides an additional support for [PsiElement]s contained
     * in dangling files that do not have backing virtual files.
     */
    public abstract fun contains(element: PsiElement): Boolean

    public companion object {
        public fun forModule(useSiteModule: KaModule): KaResolutionScope {
            return KaResolutionScopeProvider.getInstance(useSiteModule.project).getResolutionScope(useSiteModule)
        }
    }
}