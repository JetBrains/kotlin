/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.psi.psiUtil.contains

@KaImplementationDetail
class KaBaseAnalysisScopeProviderImpl(
    override val analysisSessionProvider: () -> KaSession,
    private val useSiteScope: KaResolutionScope,
) : KaBaseSessionComponent<KaSession>(), KaAnalysisScopeProvider {
    override val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { useSiteScope }

    override fun PsiElement.canBeAnalysed(): Boolean = withValidityAssertion {
        return useSiteScope.contains(this)
    }
}
