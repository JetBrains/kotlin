/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitPackageImportingScope
import org.jetbrains.kotlin.name.Name

internal class KaFirExplicitPackageImportingScope(
    firScope: FirExplicitPackageImportingScope,
    private val analysisSession: KaFirSession,
) : KaFirBasedScope<FirExplicitPackageImportingScope>(firScope, analysisSession.firSymbolBuilder) {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { emptySet() }
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion { emptySet() }
}