/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtClassInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousInitializerSymbol

internal class KtFirClassInitializerSymbol(
    override val firSymbol: FirAnonymousInitializerSymbol,
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
) : KtClassInitializerSymbol(), KtFirSymbol<FirAnonymousInitializerSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }

    override fun createPointer(): KtSymbolPointer<KtSymbol> {
        TODO("Figure out how to create such a pointer. Should we give an index to class initializers?")
    }

    override val symbolKind: KtSymbolKind get() = KtSymbolKind.CLASS_MEMBER
}