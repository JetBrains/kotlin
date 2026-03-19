/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseKDocProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.fir.deserialization.kdocText
import org.jetbrains.kotlin.psi.KtNonPublicApi

internal class KaFirKDocProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseKDocProvider<KaFirSession>() {
    @OptIn(KtNonPublicApi::class)
    override fun findDeserializedKdocText(symbol: KaDeclarationSymbol): String? {
        if (symbol.origin != KaSymbolOrigin.LIBRARY) {
            return null
        }

        return symbol.firSymbol.fir.kdocText
    }
}
