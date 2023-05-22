/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.name.Name

internal class KtFirFileScope(
    private val owner: KtFirFileSymbol,
    private val builder: KtSymbolByFirBuilder
) : KtScope {
    override val token: KtLifetimeToken get() = builder.token

    private val allNamesCached by cached {
        _callableNames + _classifierNames
    }

    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { allNamesCached }

    private val _callableNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.firSymbol.fir.declarations
            .mapNotNullTo(result) { firDeclaration ->
                when (firDeclaration) {
                    is FirSimpleFunction -> firDeclaration.name
                    is FirProperty -> firDeclaration.name
                    else -> null
                }
            }

        result
    }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { _callableNames }

    private val _classifierNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.firSymbol.fir.declarations
            .mapNotNullTo(result) { firDeclaration ->
                (firDeclaration as? FirRegularClass)?.name
            }

        result
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion { _classifierNames }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        sequence {
            owner.firSymbol.fir.declarations.forEach { firDeclaration ->
                val callableDeclaration = when (firDeclaration) {
                    is FirSimpleFunction -> firDeclaration.takeIf { nameFilter(firDeclaration.name) }
                    is FirProperty -> firDeclaration.takeIf { nameFilter(firDeclaration.name) }
                    else -> null
                }

                if (callableDeclaration != null) {
                    yield(builder.callableBuilder.buildCallableSymbol(callableDeclaration.symbol))
                }
            }
        }
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KtCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return getCallableSymbols { it in namesSet }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        sequence {
            owner.firSymbol.fir.declarations.forEach { firDeclaration ->
                val classLikeDeclaration = when (firDeclaration) {
                    is FirTypeAlias -> firDeclaration.takeIf { nameFilter(it.name) }
                    is FirRegularClass -> firDeclaration.takeIf { nameFilter(it.name) }
                    else -> null
                }
                if (classLikeDeclaration != null) {
                    yield(builder.classifierBuilder.buildClassLikeSymbol(classLikeDeclaration.symbol))
                }
            }
        }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return getClassifierSymbols { it in namesSet }
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion { emptySequence() }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        emptySequence()
    }
}