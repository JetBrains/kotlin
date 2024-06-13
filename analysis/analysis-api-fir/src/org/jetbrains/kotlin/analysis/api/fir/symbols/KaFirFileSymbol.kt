/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithDeclarations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol

internal class KaFirFileSymbol(
    override val firSymbol: FirFileSymbol,
    override val analysisSession: KaFirSession,
) : KaFileSymbol(), KaSymbolWithDeclarations, KaFirSymbol<FirFileSymbol> {
    override val psi: PsiElement? = withValidityAssertion { firSymbol.fir.psi }

    override fun createPointer(): KaSymbolPointer<KaFileSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaFileSymbol>(this)?.let { return it }
        TODO("Creating pointers for files from library is not supported yet")
    }

    override val annotations by cached {
        KaFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
