/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirTypeParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

internal class KtFirTypeParameterSymbol(
    override val firSymbol: FirTypeParameterSymbol,
    override val firResolveSession: LLFirResolveSession,
    override val token: KtLifetimeToken,
    private val builder: KtSymbolByFirBuilder
) : KtTypeParameterSymbol(), KtFirSymbol<FirTypeParameterSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion {
            KtFirAnnotationListForDeclaration.create(firSymbol, firResolveSession.useSiteFirSession, token)
        }

    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val upperBounds: List<KtType> by cached {
        firSymbol.resolvedBounds.map { type -> builder.typeBuilder.buildKtType(type) }
    }

    override val variance: Variance get() = withValidityAssertion { firSymbol.variance }
    override val isReified: Boolean get() = withValidityAssertion { firSymbol.isReified }

    override fun createPointer(): KtSymbolPointer<KtTypeParameterSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }

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
