/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.FrontendAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

interface FirKtReference : KtReference {
    fun resolveToSymbols(analysisSession: FrontendAnalysisSession): Collection<KtSymbol>

    fun getResolvedToPsi(analysisSession: FrontendAnalysisSession): Collection<PsiElement> =
        resolveToSymbols(analysisSession).mapNotNull(KtSymbol::psi)

    override val resolver get() = KtFirReferenceResolver
}