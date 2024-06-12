/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.name.Name

internal class KaFirDefaultStarImportingScope(
    firScope: FirDefaultStarImportingScope,
    analysisSession: KaFirSession,
) : KaFirBasedScope<FirDefaultStarImportingScope>(firScope, analysisSession.firSymbolBuilder) {

    private val firstWrappedScope = KaFirStarImportingScope(firScope.first, analysisSession)
    private val secondWrappedScope = KaFirStarImportingScope(firScope.second, analysisSession)

    @KaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    @KaExperimentalApi
    override fun getPossibleCallableNames(): Set<Name> = buildSet {
        addAll(firstWrappedScope.getPossibleCallableNames())
        addAll(secondWrappedScope.getPossibleCallableNames())
    }

    @KaExperimentalApi
    override fun getPossibleClassifierNames(): Set<Name> = buildSet {
        addAll(firstWrappedScope.getPossibleClassifierNames())
        addAll(secondWrappedScope.getPossibleClassifierNames())
    }
}