/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.name.Name

internal class KtFirScriptSymbol(
    override val firSymbol: FirScriptSymbol,
    override val analysisSession: KtFirAnalysisSession
) : KtScriptSymbol(), KtFirSymbol<FirScriptSymbol> {
    override val psi: PsiElement?
        get() = withValidityAssertion { firSymbol.fir.psi }

    override val annotationsList by cached {
        KtFirAnnotationListForDeclaration.create(
            firSymbol,
            analysisSession.useSiteSession,
            token,
        )
    }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    override val name: Name
        get() = withValidityAssertion { firSymbol.fir.name }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtScriptSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtScriptSymbol>(this)?.let { return it }
        TODO("Creating pointers for files from library is not supported yet")
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}