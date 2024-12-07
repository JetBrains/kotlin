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
import org.jetbrains.kotlin.analysis.api.getModule
import org.jetbrains.kotlin.analysis.api.impl.base.sessions.KaGlobalSearchScope
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.isDangling
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.contains

@KaImplementationDetail
class KaBaseAnalysisScopeProviderImpl(
    override val analysisSessionProvider: () -> KaSession,
    private val useSiteScope: KaGlobalSearchScope,
) : KaSessionComponent<KaSession>(), KaAnalysisScopeProvider {
    override val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { useSiteScope }

    override fun PsiElement.canBeAnalysed(): Boolean = withValidityAssertion {
        return (useSiteScope.baseScope.contains(this) && !useSiteScope.shadowedScope.contains(this)) || this.isFromGeneratedModule()
    }

    private fun PsiElement.isFromGeneratedModule(): Boolean {
        val ktFile = containingFile as? KtFile ?: return false
        if (ktFile.isDangling) {
            val module = analysisSession.getModule(ktFile)
            return useSiteScope.isFromGeneratedModule(module)
        }

        val virtualFile = ktFile.virtualFile ?: return false
        return useSiteScope.isFromGeneratedModule(virtualFile)
    }
}
