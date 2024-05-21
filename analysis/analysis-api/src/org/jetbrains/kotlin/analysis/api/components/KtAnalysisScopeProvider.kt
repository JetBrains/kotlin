/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion

public abstract class KaAnalysisScopeProvider : KaSessionComponent() {
    public abstract fun getAnalysisScope(): GlobalSearchScope

    public abstract fun canBeAnalysed(psi: PsiElement): Boolean
}

public typealias KtAnalysisScopeProvider = KaAnalysisScopeProvider

public interface KaAnalysisScopeProviderMixIn : KaSessionMixIn {
    /**
     * Return [GlobalSearchScope] represent a scope code in which can be analysed by current [KaSession].
     * That means [org.jetbrains.kotlin.analysis.api.symbols.KaSymbol] can be built for the declarations from this scope.
     */
    public val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { analysisSession.analysisScopeProvider.getAnalysisScope() }


    /**
     * Checks if [PsiElement] is inside analysis scope.
     * That means [org.jetbrains.kotlin.analysis.api.symbols.KaSymbol] can be built by this [PsiElement]
     *
     * @see analysisScope
     */
    public fun PsiElement.canBeAnalysed(): Boolean =
        withValidityAssertion { analysisSession.analysisScopeProvider.canBeAnalysed(this) }
}

public typealias KtAnalysisScopeProviderMixIn = KaAnalysisScopeProviderMixIn