/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

public interface KaAnalysisScopeProvider {
    /**
     * A [GlobalSearchScope] which spans the files that can be analyzed by the current [KaSession].
     *
     * For example, [KaSymbol]s can only be built for declarations which are in the analysis scope.
     */
    public val analysisScope: GlobalSearchScope

    /**
     * Checks whether the [PsiElement] is inside the [analysisScope].
     *
     * For example, a [KaSymbol] can only be built for this [PsiElement] if it can be analyzed.
     */
    public fun PsiElement.canBeAnalysed(): Boolean
}
