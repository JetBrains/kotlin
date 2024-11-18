/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol
import org.jetbrains.kotlin.psi.KtClassInitializer

internal class KaFirClassInitializerSymbol private constructor(
    override val backingPsi: KtClassInitializer?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirAnonymousInitializerSymbol>,
) : KaClassInitializerSymbol(), KaFirKtBasedSymbol<KtClassInitializer, FirAnonymousInitializerSymbol> {
    constructor(declaration: KtClassInitializer, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    override val psi: PsiElement? get() = withValidityAssertion { backingPsi ?: firSymbol.fir.getAllowedPsi() }

    override fun createPointer(): KaSymbolPointer<KaClassInitializerSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaClassInitializerSymbol>(analysisSession.project)?.let { return it }

        TODO("Figure out how to create such a pointer. Should we give an index to class initializers?")
    }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
