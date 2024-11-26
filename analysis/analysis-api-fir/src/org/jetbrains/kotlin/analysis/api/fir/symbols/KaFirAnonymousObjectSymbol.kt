/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.psi.KtObjectDeclaration

internal open class KaFirAnonymousObjectSymbol private constructor(
    override val backingPsi: KtObjectDeclaration?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirAnonymousObjectSymbol>,
) : KaAnonymousObjectSymbol(), KaFirKtBasedSymbol<KtObjectDeclaration, FirAnonymousObjectSymbol> {
    init {
        require(backingPsi?.isObjectLiteral() != false)
    }

    constructor(declaration: KtObjectDeclaration, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirAnonymousObjectSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtObjectDeclaration,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi ?: firSymbol.fir.getAllowedPsi() }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            psiOrSymbolAnnotationList()
        }

    override val superTypes: List<KaType>
        get() = withValidityAssertion { createSuperTypes() }

    override fun createPointer(): KaSymbolPointer<KaAnonymousObjectSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaAnonymousObjectSymbol>()?.let { return it }

        throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(this::class)
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
