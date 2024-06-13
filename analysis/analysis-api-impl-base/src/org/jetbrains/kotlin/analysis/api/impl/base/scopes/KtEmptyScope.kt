/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.scopes

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.Name

class KaEmptyScope(override val token: KaLifetimeToken) : KaScope {
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getAllSymbols(): Sequence<KaDeclarationSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getCallableSymbols(nameFilter: KaScopeNameFilter): Sequence<KaCallableSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getClassifierSymbols(nameFilter: KaScopeNameFilter): Sequence<KaClassifierSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        return false
    }
}