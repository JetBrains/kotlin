/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirTypeParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.UnexpandedTypeCheck
import org.jetbrains.kotlin.fir.types.isNullableAny

/**
 * [KtFirTypeParameterSymbolBase] provides shared implementations for [KtFirTypeParameterSymbol] and [KtFirPsiJavaTypeParameterSymbol].
 */
internal sealed class KtFirTypeParameterSymbolBase : KtTypeParameterSymbol(), KtFirSymbol<FirTypeParameterSymbol> {
    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion {
            KtFirAnnotationListForDeclaration.create(firSymbol, builder)
        }

    @OptIn(UnexpandedTypeCheck::class)
    override val upperBounds: List<KtType> by cached {
        firSymbol.resolvedBounds.mapNotNull { type ->
            if (type.isNullableAny) return@mapNotNull null
            builder.typeBuilder.buildKtType(type)
        }
    }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtTypeParameterSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtTypeParameterSymbol>(this)?.let { return it }

        val containingDeclarationSymbol = firSymbol.containingDeclarationSymbol
        val typeParameters = containingDeclarationSymbol.typeParameterSymbols
        requireNotNull(typeParameters) { "Containing declaration symbol: ${containingDeclarationSymbol::class.simpleName}" }

        KtFirTypeParameterSymbolPointer(
            ownerPointer = requireOwnerPointer(),
            name = name,
            index = typeParameters.indexOf(firSymbol),
        )
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
