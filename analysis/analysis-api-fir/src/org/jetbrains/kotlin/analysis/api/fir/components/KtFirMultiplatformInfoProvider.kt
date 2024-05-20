/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaMultiplatformInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

internal class KaFirMultiplatformInfoProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken,
) : KaMultiplatformInfoProvider(), KaFirSessionComponent {
    override fun getExpectForActual(actual: KaDeclarationSymbol): List<KaDeclarationSymbol> {
        require(actual is KaFirSymbol<*>)
        val firSymbol = actual.firSymbol
        if (firSymbol !is FirCallableSymbol && firSymbol !is FirClassSymbol && firSymbol !is FirTypeAliasSymbol) {
            return emptyList()
        }

        return firSymbol.expectForActual?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)
            ?.map { analysisSession.firSymbolBuilder.buildSymbol(it) as KaDeclarationSymbol }.orEmpty()
    }
}
