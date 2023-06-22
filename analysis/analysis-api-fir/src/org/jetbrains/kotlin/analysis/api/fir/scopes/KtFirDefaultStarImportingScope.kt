/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.name.Name

internal class KtFirDefaultStarImportingScope(
    firScope: FirDefaultStarImportingScope,
    analysisSession: KtFirAnalysisSession,
) : KtFirBasedScope<FirDefaultStarImportingScope>(firScope, analysisSession.firSymbolBuilder) {

    private val firstWrappedScope = KtFirStarImportingScope(firScope.first, analysisSession)
    private val secondWrappedScope = KtFirStarImportingScope(firScope.second, analysisSession)

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion { emptySequence() }

    override fun getPossibleCallableNames(): Set<Name> = buildSet {
        addAll(firstWrappedScope.getPossibleCallableNames())
        addAll(secondWrappedScope.getPossibleCallableNames())
    }

    override fun getPossibleClassifierNames(): Set<Name> = buildSet {
        addAll(firstWrappedScope.getPossibleClassifierNames())
        addAll(secondWrappedScope.getPossibleClassifierNames())
    }
}