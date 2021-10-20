/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.scopes.KtDelegatedMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope

internal class KtFirDelegatedMemberScope(
    override val owner: KtSymbolWithMembers,
    override val firScope: FirContainingNamesAwareScope,
    token: ValidityToken,
    builder: KtSymbolByFirBuilder
) : KtFirDelegatingScope<FirContainingNamesAwareScope>(builder, token),
    KtDelegatedMemberScope,
    ValidityTokenOwner {

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> {
        return super.getCallableSymbols(nameFilter).filter { it.origin == KtSymbolOrigin.DELEGATED }
    }
}
