/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.location
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.types.Variance

internal class KaFirTypeParameterSymbol private constructor(
    override val backingPsi: KtTypeParameter?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirTypeParameterSymbol>,
) : KaFirTypeParameterSymbolBase<KtTypeParameter>(), KaFirKtBasedSymbol<KtTypeParameter, FirTypeParameterSymbol> {
    constructor(declaration: KtTypeParameter, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirTypeParameterSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtTypeParameter,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val variance: Variance
        get() = withValidityAssertion { backingPsi?.variance ?: firSymbol.variance }

    override val isReified: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.REIFIED_KEYWORD) ?: firSymbol.isReified }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            when {
                backingPsi != null -> backingPsi.location
                firSymbol.containingDeclarationSymbol is FirClassSymbol<*> -> KaSymbolLocation.CLASS
                else -> KaSymbolLocation.LOCAL
            }
        }
}
