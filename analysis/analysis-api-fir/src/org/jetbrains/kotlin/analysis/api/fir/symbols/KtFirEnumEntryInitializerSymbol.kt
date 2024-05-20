/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirEnumEntryInitializerSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol

internal class KaFirEnumEntryInitializerSymbol(
    firSymbol: FirAnonymousObjectSymbol,
    analysisSession: KaFirSession,
) : KaFirAnonymousObjectSymbol(firSymbol, analysisSession), KaEnumEntryInitializerSymbol {
    init {
        check(firSymbol.source?.kind == KtFakeSourceElementKind.EnumInitializer) {
            "Expected the `firSymbol` of ${KaFirEnumEntryInitializerSymbol::class.simpleName} to have an enum initializer fake source kind."
        }
    }

    /**
     * [KaFirEnumEntryInitializerSymbol] is the required return type instead of [KaEnumEntryInitializerSymbol] to fulfill return type
     * subtyping requirements, as [KaEnumEntryInitializerSymbol] is not a subtype of
     * [org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol]. (It cannot be a subtype in the general Analysis API because enum
     * entry initializers are classes in FE10.)
     */
    override fun createPointer(): KaSymbolPointer<KaFirEnumEntryInitializerSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaFirEnumEntryInitializerSymbol>(this)
            ?: KaFirEnumEntryInitializerSymbolPointer(analysisSession.createOwnerPointer(this))
    }
}
