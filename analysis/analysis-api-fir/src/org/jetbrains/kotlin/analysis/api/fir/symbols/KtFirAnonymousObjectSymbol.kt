/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol

internal open class KaFirAnonymousObjectSymbol(
    override val firSymbol: FirAnonymousObjectSymbol,
    override val analysisSession: KaFirSession,
) : KaAnonymousObjectSymbol(), KaFirSymbol<FirAnonymousObjectSymbol> {
    override val psi: PsiElement? = withValidityAssertion { firSymbol.fir.getAllowedPsi() }

    override val annotationsList by cached {
        KaFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    override val superTypes: List<KaType> by cached { firSymbol.superTypesList(builder) }

    override fun createPointer(): KaSymbolPointer<KaAnonymousObjectSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaAnonymousObjectSymbol>(this)?.let { return it }

        throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(this::class)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
