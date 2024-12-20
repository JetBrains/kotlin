/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirTypeParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.UnexpandedTypeCheck
import org.jetbrains.kotlin.fir.types.isNullableAny

/**
 * [KaFirTypeParameterSymbolBase] provides shared implementations for [KaFirTypeParameterSymbol] and [KaFirPsiJavaTypeParameterSymbol].
 */
internal sealed class KaFirTypeParameterSymbolBase<P : PsiElement> : KaTypeParameterSymbol(), KaFirPsiSymbol<P, FirTypeParameterSymbol> {
    @OptIn(UnexpandedTypeCheck::class)
    override val upperBounds: List<KaType>
        get() = withValidityAssertion {
            firSymbol.resolvedBounds.mapNotNull { type ->
                if (type.isNullableAny) return@mapNotNull null
                builder.typeBuilder.buildKtType(type)
            }
        }

    override fun createPointer(): KaSymbolPointer<KaTypeParameterSymbol> = withValidityAssertion {
        if (this is KaFirKtBasedSymbol<*, *>) {
            psiBasedSymbolPointerOfTypeIfSource<KaTypeParameterSymbol>()?.let { return it }
        }

        val containingDeclarationSymbol = firSymbol.containingDeclarationSymbol
        val typeParameters = containingDeclarationSymbol.typeParameterSymbols
        requireNotNull(typeParameters) { "Containing declaration symbol: ${containingDeclarationSymbol::class.simpleName}" }

        KaFirTypeParameterSymbolPointer(
            ownerPointer = analysisSession.createOwnerPointer(this),
            name = name,
            index = typeParameters.indexOf(firSymbol),
            originalSymbol = this
        )
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
