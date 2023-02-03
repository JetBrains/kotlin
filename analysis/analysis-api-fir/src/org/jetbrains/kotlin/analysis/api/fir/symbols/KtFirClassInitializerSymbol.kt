/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol

internal class KtFirClassInitializerSymbol(
    override val firSymbol: FirAnonymousInitializerSymbol,
    override val analysisSession: KtFirAnalysisSession,
) : KtClassInitializerSymbol(), KtFirSymbol<FirAnonymousInitializerSymbol> {
    override val psi: PsiElement? = withValidityAssertion { firSymbol.fir.getAllowedPsi() }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtClassInitializerSymbol>(this)?.let { return it }
        TODO("Figure out how to create such a pointer. Should we give an index to class initializers?")
    }

    override val symbolKind: KtSymbolKind get() = withValidityAssertion { KtSymbolKind.CLASS_MEMBER }

    override val typeParameters: List<KtTypeParameterSymbol> get() = withValidityAssertion { emptyList() }
    override val annotationsList: KtAnnotationsList get() = withValidityAssertion { KtEmptyAnnotationsList(token) }
}