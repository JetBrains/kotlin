/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.name.Name

internal class KaFirFileScope(
    private val owner: KaFirFileSymbol,
    private val builder: KaSymbolByFirBuilder
) : KaScope {
    override val token: KaLifetimeToken get() = builder.token

    private val allNamesCached by cached {
        backingCallableNames + _classifierNames
    }

    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { allNamesCached }

    @OptIn(DirectDeclarationsAccess::class)
    private val backingCallableNames: Set<Name> by cached {
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

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { backingCallableNames }

    @OptIn(DirectDeclarationsAccess::class)
    private val _classifierNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.firSymbol.fir.declarations
            .mapNotNullTo(result) { firDeclaration ->
                (firDeclaration as? FirRegularClass)?.name
            }

        result
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion { _classifierNames }

    @OptIn(DirectDeclarationsAccess::class)
    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = withValidityAssertion {
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

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return callables { it in namesSet }
    }

    @OptIn(DirectDeclarationsAccess::class)
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = withValidityAssertion {
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

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return classifiers { it in namesSet }
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }
}