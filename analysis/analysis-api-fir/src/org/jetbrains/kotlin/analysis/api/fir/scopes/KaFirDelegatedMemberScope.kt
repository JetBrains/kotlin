/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.Name

/**
 * The callables that the class [owner] implements by delegation, taken from its member scope [firScope], so that they are the same
 * symbols as the ones in the member scope.
 *
 * Only the [callable names of the delegate types][delegateCallableNames] are looked up in [firScope], since the member scope also has the
 * names of all inherited members.
 */
internal class KaFirDelegatedMemberScope(
    firScope: FirContainingNamesAwareScope,
    builder: KaSymbolByFirBuilder,
    private val owner: ConeClassLikeLookupTag,
    private val delegateCallableNames: Set<Name>,
) : KaFirDelegatingNamesAwareScope(firScope, builder) {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { delegateCallableNames }

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = withValidityAssertion {
        return super.callables(nameFilter).filter { it.isDelegatedBy(owner) }
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        return super.callables(names).filter { it.isDelegatedBy(owner) }
    }
}

/**
 * Whether this callable is implemented by delegation in the class [owner] itself rather than inherited from a superclass.
 */
private fun KaCallableSymbol.isDelegatedBy(owner: ConeClassLikeLookupTag): Boolean =
    origin == KaSymbolOrigin.DELEGATED && firSymbol.containingClassLookupTag() == owner
