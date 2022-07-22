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
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependencies
import org.jetbrains.kotlin.psi.psiUtil.contains

class KtAnalysisScopeProviderImpl(
    override val analysisSession: KtAnalysisSession,
    override val token: KtLifetimeToken
) : KtAnalysisScopeProvider() {

    private val allModules = buildList {
        add(analysisSession.useSiteModule)
        addAll(analysisSession.useSiteModule.allDirectDependencies())
    }

    private val analysisScope = GlobalSearchScope.union(allModules.map { it.contentScope })

    override fun getAnalysisScope(): GlobalSearchScope = analysisScope

    override fun canBeAnalysed(psi: PsiElement): Boolean {
        return analysisScope.contains(psi)
    }
}
