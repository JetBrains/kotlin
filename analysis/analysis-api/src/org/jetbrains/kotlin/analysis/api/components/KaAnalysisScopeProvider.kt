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
     * A [GlobalSearchScope] containing files analyzable by the current [KaSession].
     * It means that [KaSymbol]s can be built for declarations from the scope.
     */
    public val analysisScope: GlobalSearchScope

    /**
     * Checks whether the receiver [PsiElement] is inside the analysis scope.
     * It means that a [KaSymbol] can be potentially built using this [PsiElement].
     *
     * @see analysisScope
     */
    public fun PsiElement.canBeAnalysed(): Boolean
}