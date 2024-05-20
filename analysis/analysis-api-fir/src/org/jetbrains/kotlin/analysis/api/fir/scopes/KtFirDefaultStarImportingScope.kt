/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
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

    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion { emptySequence() }

    override fun getPossibleCallableNames(): Set<Name> = buildSet {
        addAll(firstWrappedScope.getPossibleCallableNames())
        addAll(secondWrappedScope.getPossibleCallableNames())
    }

    override fun getPossibleClassifierNames(): Set<Name> = buildSet {
        addAll(firstWrappedScope.getPossibleClassifierNames())
        addAll(secondWrappedScope.getPossibleClassifierNames())
    }
}