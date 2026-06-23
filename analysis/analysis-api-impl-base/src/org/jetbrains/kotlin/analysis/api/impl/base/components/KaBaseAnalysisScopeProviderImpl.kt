/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsAnalysisScopeProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScope

@KaImplementationDetail
class KaBaseAnalysisScopeProviderImpl(
    override val analysisSessionProvider: () -> KaSession,
    private val resolutionScope: KaResolutionScope,
) : KaBaseSessionComponent<KaSession>(), KaInternalsAnalysisScopeProvider {
    override val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { resolutionScope }

    override fun canBeAnalysed(element: PsiElement): Boolean = withValidityAssertion {
        canBeAnalysedImpl(element)
    }

    override fun canBeAnalysedImpl(element: PsiElement): Boolean {
        return resolutionScope.contains(element)
    }
}
