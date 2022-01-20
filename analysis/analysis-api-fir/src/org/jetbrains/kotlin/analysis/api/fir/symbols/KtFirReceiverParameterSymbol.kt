/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

internal class KtFirReceiverParameterSymbol(
    val firSymbol: FirCallableSymbol<*>,
    val resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder
) : KtReceiverParameterSymbol(), ValidityTokenOwner {
    override val psi: PsiElement? by cached { firSymbol.fir.receiverTypeRef?.findPsi(firSymbol.fir.moduleData.session) }

    init {
        require(firSymbol.fir.receiverTypeRef != null) { "$firSymbol doesn't have an extension receiver." }
    }

    private val builder by weakRef(_builder)

    override val type: KtType by cached {
        firSymbol.receiverType(builder)
            ?: error("$firSymbol doesn't have an extension receiver.")
    }

    override val origin: KtSymbolOrigin = withValidityAssertion { firSymbol.fir.ktSymbolOrigin() }

    override fun createPointer(): KtSymbolPointer<KtFirReceiverParameterSymbol> {
        TODO("Probably just create a pointer based on the underlying declaration")
    }
}