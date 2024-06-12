/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.name.Name

internal open class KaFirDelegatingNamesAwareScope(
    firScope: FirContainingNamesAwareScope,
    builder: KaSymbolByFirBuilder,
) : KaFirBasedScope<FirContainingNamesAwareScope>(firScope, builder) {
    @OptIn(KaExperimentalApi::class)
    private val allNamesCached by cached {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    @KaExperimentalApi
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { allNamesCached }

    @KaExperimentalApi
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        firScope.getCallableNames()
    }

    @KaExperimentalApi
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        firScope.getClassifierNames()
    }

    @KaExperimentalApi
    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getAllPossibleNames()
    }
}
