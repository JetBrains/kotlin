/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtAnalysisScopeProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.providers.KotlinResolutionScopeProvider
import org.jetbrains.kotlin.psi.psiUtil.contains

class KtAnalysisScopeProviderImpl(
    override val analysisSession: KtAnalysisSession,
    override val token: KtLifetimeToken
) : KtAnalysisScopeProvider() {

    private val scope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinResolutionScopeProvider.getInstance(analysisSession.useSiteModule.project).getResolutionScope(analysisSession.useSiteModule)
    }

    override fun getAnalysisScope(): GlobalSearchScope = scope

    override fun canBeAnalysed(psi: PsiElement): Boolean {
        return scope.contains(psi)
    }
}
