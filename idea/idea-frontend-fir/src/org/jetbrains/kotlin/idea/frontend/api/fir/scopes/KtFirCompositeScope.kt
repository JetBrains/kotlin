/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtCompositeScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name


// todo do we need caches here?
@OptIn(ExperimentalStdlibApi::class)
class KtFirCompositeScope(
    override val subScopes: List<KtScope>,
    override val token: ValidityToken
) : KtCompositeScope, ValidityTokenOwner {
    override fun getAllNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getAllNames() }
        }
    }

    override fun getCallableNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getCallableNames() }
        }
    }

    override fun getClassifierNames(): Set<Name> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getClassifierNames() }
        }
    }

    override fun getAllSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getAllSymbols()) }
        }
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getCallableSymbols(nameFilter)) }
        }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        sequence {
            subScopes.forEach { yieldAll(it.getClassifierSymbols(nameFilter)) }
        }
    }

    override fun containsName(name: Name): Boolean = withValidityAssertion {
        subScopes.any { it.containsName(name) }
    }
}