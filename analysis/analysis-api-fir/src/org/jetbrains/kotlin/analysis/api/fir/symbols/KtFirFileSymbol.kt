/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol

internal class KtFirFileSymbol(
    override val firSymbol: FirFileSymbol,
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
) : KtFileSymbol(), KtSymbolWithDeclarations, KtFirSymbol<FirFileSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override fun createPointer(): KtSymbolPointer<KtFileSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        TODO("Creating pointers for files from library is not supported yet")
    }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, resolveState.rootModuleSession, token) }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
