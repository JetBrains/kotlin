/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.session.analysisScope as analysisScopeEndpoint
import org.jetbrains.kotlin.analysis.api.session.canBeAnalysed as canBeAnalysedEndpoint

internal class KaAnalysisScopeProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaFirSessionComponent(), KaAnalysisScopeProvider {
    override val analysisScope: GlobalSearchScope
        get() = context(analysisSession) { analysisScopeEndpoint }

    override fun PsiElement.canBeAnalysed(): Boolean =
        context(analysisSession) { canBeAnalysedEndpoint() }
}
