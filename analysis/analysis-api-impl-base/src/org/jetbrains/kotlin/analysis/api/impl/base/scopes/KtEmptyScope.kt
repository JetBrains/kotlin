/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name

class KtEmptyScope(override val token: KtLifetimeToken) : KtScope {
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getAllSymbols(): Sequence<KtDeclarationSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KtCallableSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol>  = withValidityAssertion {
        return emptySequence()
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        return false
    }
}