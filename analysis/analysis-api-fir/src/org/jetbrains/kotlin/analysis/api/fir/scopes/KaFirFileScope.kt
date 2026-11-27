/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isSynthetic
import org.jetbrains.kotlin.name.Name

/**
 * Synthetic properties, such as destructuring declaration containers, are skipped, as declared member scopes skip them too.
 */
internal class KaFirFileScope(
    private val owner: KaFirFileSymbol,
    override val analysisSession: KaFirSession
) : KaFirScope(), KaFirSessionBoundOwner {

    private val allNamesCached by cached {
        backingCallableNames + _classifierNames
    }

    override fun getAllPossibleNames(): Set<Name> = entryPointImplementation { allNamesCached }

    @OptIn(DirectDeclarationsAccess::class)
    private val backingCallableNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.firSymbol.fir.declarations
            .mapNotNullTo(result) { firDeclaration ->
                when (firDeclaration) {
                    is FirNamedFunction -> firDeclaration.name
                    is FirProperty -> firDeclaration.name.takeUnless { firDeclaration.isSynthetic }
                    else -> null
                }
            }

        result
    }

    override fun getPossibleCallableNames(): Set<Name> = entryPointImplementation { backingCallableNames }

    @OptIn(DirectDeclarationsAccess::class)
    private val _classifierNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.firSymbol.fir.declarations
            .mapNotNullTo(result) { firDeclaration ->
                (firDeclaration as? FirRegularClass)?.name
            }

        result
    }

    override fun getPossibleClassifierNames(): Set<Name> = entryPointImplementation { _classifierNames }

    @OptIn(DirectDeclarationsAccess::class)
    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = entryPointImplementation {
        sequence {
            owner.firSymbol.fir.declarations.forEach { firDeclaration ->
                val callableDeclaration = when (firDeclaration) {
                    is FirNamedFunction -> firDeclaration.takeIf { nameFilter(firDeclaration.name) }
                    is FirProperty -> firDeclaration.takeIf { !it.isSynthetic && nameFilter(it.name) }
                    else -> null
                }

                if (callableDeclaration != null) {
                    yield(firSymbolBuilder.callableBuilder.buildCallableSymbol(callableDeclaration.symbol))
                }
            }
        }
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = entryPointImplementation {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return callables { it in namesSet }
    }

    @OptIn(DirectDeclarationsAccess::class)
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = entryPointImplementation {
        sequence {
            owner.firSymbol.fir.declarations.forEach { firDeclaration ->
                val classLikeDeclaration = when (firDeclaration) {
                    is FirTypeAlias -> firDeclaration.takeIf { nameFilter(it.name) }
                    is FirRegularClass -> firDeclaration.takeIf { nameFilter(it.name) }
                    else -> null
                }
                if (classLikeDeclaration != null) {
                    yield(firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(classLikeDeclaration.symbol))
                }
            }
        }
    }

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = entryPointImplementation {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return classifiers { it in namesSet }
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = entryPointImplementation { emptySequence() }

    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = entryPointImplementation {
        emptySequence()
    }
}
