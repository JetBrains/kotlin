/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.scopes

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal open class KtFe10EmptyScope(override val token: ValidityToken) : KtScope, ValidityTokenOwner {
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        return emptySet()
    }

    override fun getAllSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        return emptySequence()
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        return false
    }
}