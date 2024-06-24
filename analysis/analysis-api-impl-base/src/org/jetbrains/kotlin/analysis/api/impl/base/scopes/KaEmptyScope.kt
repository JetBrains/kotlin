/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name

class KaEmptyScope(override val token: KaLifetimeToken) : KaScope {
    @KaExperimentalApi
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    @KaExperimentalApi
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    @KaExperimentalApi
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override val declarations: Sequence<KaDeclarationSymbol>
        get() = withValidityAssertion { emptySequence() }

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    @KaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    @KaExperimentalApi
    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        return false
    }
}
