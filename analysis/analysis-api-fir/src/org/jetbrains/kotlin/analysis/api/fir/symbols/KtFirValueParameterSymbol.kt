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
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.name.Name

internal class KtFirValueParameterSymbol(
    override val firSymbol: FirValueParameterSymbol,
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtValueParameterSymbol(), KtFirSymbol<FirValueParameterSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val isVararg: Boolean get() = withValidityAssertion { firSymbol.isVararg }

    override val returnType by cached {
        val returnType = firSymbol.resolvedReturnType
        return@cached if (firSymbol.isVararg) {
            // There SHOULD always be an array element type (even if it is an error type, e.g., unresolved).
            val arrayElementType = returnType.arrayElementType()
                ?: error("No array element type for vararg value parameter: ${firSymbol.fir.renderWithType()}")
            builder.typeBuilder.buildKtType(arrayElementType)
        } else {
            builder.typeBuilder.buildKtType(returnType)
        }
    }

    override val hasDefaultValue: Boolean get() = withValidityAssertion { firSymbol.hasDefaultValue }

    override val annotationsList by cached { KtFirAnnotationListForDeclaration.create(firSymbol, resolveState.rootModuleSession, token) }

    override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> {
        KtPsiBasedSymbolPointer.createForSymbolFromSource(this)?.let { return it }
        TODO("Creating pointers for functions parameters from library is not supported yet")
    }

    override fun equals(other: Any?): Boolean = symbolEquals(other)
    override fun hashCode(): Int = symbolHashCode()
}
