/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

internal class KtFirLocalVariableSymbol(
    override val firSymbol: FirPropertySymbol,
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtLocalVariableSymbol(),
    KtFirSymbol<FirPropertySymbol> {
    init {
        assert(firSymbol.isLocal)
    }

    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val isVal: Boolean get() = withValidityAssertion { firSymbol.isVal }
    override val name: Name get() = withValidityAssertion { firSymbol.name }
    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL

    override fun createPointer(): KtSymbolPointer<KtLocalVariableSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(name.asString())
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
