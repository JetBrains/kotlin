/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.name.Name

internal class KaFirDelegatedMemberScope(
    firScope: FirContainingNamesAwareScope,
    analysisSession: KaFirSession
) : KaFirDelegatingNamesAwareScope(firScope, analysisSession) {
    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = entryPointImplementation {
        return super.callables(nameFilter).filter { it.origin == KaSymbolOrigin.DELEGATED }
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = entryPointImplementation {
        return super.callables(names).filter { it.origin == KaSymbolOrigin.DELEGATED }
    }
}
