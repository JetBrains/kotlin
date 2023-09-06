/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirEnumEntryInitializerSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.requireOwnerPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol

internal class KtFirEnumEntryInitializerSymbol(
    firSymbol: FirAnonymousObjectSymbol,
    analysisSession: KtFirAnalysisSession,
) : KtFirAnonymousObjectSymbol(firSymbol, analysisSession), KtEnumEntryInitializerSymbol {
    init {
        check(firSymbol.source?.kind == KtFakeSourceElementKind.EnumInitializer) {
            "Expected the `firSymbol` of ${KtFirEnumEntryInitializerSymbol::class.simpleName} to have an enum initializer fake source kind."
        }
    }

    /**
     * [KtFirEnumEntryInitializerSymbol] is the required return type instead of [KtEnumEntryInitializerSymbol] to fulfill return type
     * subtyping requirements, as [KtEnumEntryInitializerSymbol] is not a subtype of
     * [org.jetbrains.kotlin.analysis.api.symbols.KtAnonymousObjectSymbol]. (It cannot be a subtype in the general Analysis API because enum
     * entry initializers are classes in FE10.)
     */
    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtFirEnumEntryInitializerSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtFirEnumEntryInitializerSymbol>(this)?.let { return it }

        KtFirEnumEntryInitializerSymbolPointer(requireOwnerPointer())
    }
}
