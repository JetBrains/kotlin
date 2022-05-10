/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol

internal class KtFirAnonymousObjectSymbol(
    override val firSymbol: FirAnonymousObjectSymbol,
    override val firResolveSession: LLFirResolveSession,
    override val token: KtLifetimeToken,
    private val builder: KtSymbolByFirBuilder,
) : KtAnonymousObjectSymbol(), KtFirSymbol<FirAnonymousObjectSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, firResolveSession.useSiteFirSession, token) }

    override val superTypes: List<KtType> by cached { firSymbol.superTypesList(builder) }

    override fun createPointer(): KtSymbolPointer<KtAnonymousObjectSymbol> =
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)
            ?: throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException("Cannot create pointer for KtFirAnonymousObjectSymbol")

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
