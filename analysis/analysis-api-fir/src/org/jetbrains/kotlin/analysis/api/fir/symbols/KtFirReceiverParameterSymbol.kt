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
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration

internal class KtFirReceiverParameterSymbol(
    fir: FirCallableDeclaration,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    _builder: KtSymbolByFirBuilder
) : KtReceiverParameterSymbol(), ValidityTokenOwner {
    init {
        require(fir.receiverTypeRef != null) { "$fir doesn't have an extension receiver." }
    }

    private val builder by weakRef(_builder)
    internal val firRef = firRef(fir, resolveState)

    override val type: KtType by cached {
        firRef.receiverType(builder) ?: throw IllegalStateException("$fir doesn't have an extension receiver.")
    }
    override val origin: KtSymbolOrigin get() = firRef.withFir { it.ktSymbolOrigin() }

    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.receiverTypeRef?.findPsi(fir.moduleData.session) }

    override fun createPointer(): KtSymbolPointer<KtFirReceiverParameterSymbol> {
        TODO("Probably just create a pointer based on the underlying declaration")
    }
}