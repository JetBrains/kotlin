/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirScriptSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirScriptSymbol(
    override val firSymbol: FirScriptSymbol,
    override val analysisSession: KaFirSession
) : KaScriptSymbol(), KaFirSymbol<FirScriptSymbol> {
    override val psi: PsiElement?
        get() = withValidityAssertion { firSymbol.fir.psi }

    override val annotations by cached {
        KaFirAnnotationListForDeclaration.create(firSymbol, builder)
    }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    override val name: Name
        get() = withValidityAssertion { firSymbol.fir.name }

    override fun createPointer(): KaSymbolPointer<KaScriptSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaScriptSymbol>(this)?.let { return it }

        val firFile = firSymbol.fir.getContainingFile() ?: errorWithAttachment("${FirFile::class.simpleName} is not found") {
            withFirSymbolEntry("script", firSymbol)
        }

        firFile.symbol.let(builder::buildFileSymbol).createPointer().let(::KaFirScriptSymbolPointer)
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}