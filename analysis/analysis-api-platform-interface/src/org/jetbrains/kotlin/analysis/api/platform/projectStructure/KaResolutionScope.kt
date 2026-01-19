/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * The resolution scope for a [KaModule]. It determines the PSI elements that can be resolved in the context of an analysis session.
 *
 * A resolution scope is mainly built from its use-site module's [KaModule.contentScope] and its dependencies' content scopes.
 */
@KaPlatformInterface
public abstract class KaResolutionScope : GlobalSearchScope() {
    /**
     * Checks whether a [PsiElement] is contained in the resolution scope, i.e., the containing file is present in the resolution scope.
     *
     * In contrast to [GlobalSearchScope.contains], this method provides additional support for [PsiElement]s contained in dangling files
     * that do not have backing virtual files.
     */
    public abstract fun contains(element: PsiElement): Boolean

    /**
     * The underlying [GlobalSearchScope] that covers all physical files contained in the resolution scope. **Only intended for test
     * purposes.**
     */
    @KaImplementationDetail
    public abstract val underlyingSearchScope: GlobalSearchScope

    @KaPlatformInterface
    public companion object {
        public fun forModule(useSiteModule: KaModule): KaResolutionScope =
            KaResolutionScopeProvider.getInstance(useSiteModule.project).getResolutionScope(useSiteModule)
    }
}
