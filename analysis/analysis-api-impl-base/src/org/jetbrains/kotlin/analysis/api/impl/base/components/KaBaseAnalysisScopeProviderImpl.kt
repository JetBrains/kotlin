/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScope

@KaImplementationDetail
class KaBaseAnalysisScopeProviderImpl(
    override val analysisSessionProvider: () -> KaSession,
    private val resolutionScope: KaResolutionScope,
) : KaBaseSessionComponent<KaSession>(), KaBaseAnalysisScopeProviderEx {
    override val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { resolutionScope }

    override fun canBeAnalysedImpl(element: PsiElement): Boolean {
        return resolutionScope.contains(element)
    }
}

/**
 * The implementation detail of [KaAnalysisScopeProvider] which exposes the internal implementation details
 * to reuse it in other places.
 */
@KaImplementationDetail
interface KaBaseAnalysisScopeProviderEx : KaAnalysisScopeProvider {
    /**
     * The implementation of [canBeAnalysed] without [withValidityAssertion] check.
     *
     * @see canBeAnalysed
     */
    fun canBeAnalysedImpl(element: PsiElement): Boolean

    override fun PsiElement.canBeAnalysed(): Boolean = withValidityAssertion {
        canBeAnalysedImpl(this)
    }
}
