/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol

internal class KaFirClassInitializerSymbol(
    override val firSymbol: FirAnonymousInitializerSymbol,
    override val analysisSession: KaFirSession,
) : KaClassInitializerSymbol(), KaFirSymbol<FirAnonymousInitializerSymbol> {
    override val psi: PsiElement? = withValidityAssertion { firSymbol.fir.getAllowedPsi() }

    override fun createPointer(): KaSymbolPointer<KaSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaClassInitializerSymbol>(this)?.let { return it }
        TODO("Figure out how to create such a pointer. Should we give an index to class initializers?")
    }

    override val symbolKind: KaSymbolKind get() = withValidityAssertion { KaSymbolKind.CLASS_MEMBER }

    override val typeParameters: List<KaTypeParameterSymbol> get() = withValidityAssertion { emptyList() }
    override val annotationsList: KaAnnotationsList get() = withValidityAssertion { KaEmptyAnnotationsList(token) }
}