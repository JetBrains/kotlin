/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.fir.isSubstitutionOverride
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name

internal open class KtFirDelegatingScope(
    val firScope: FirContainingNamesAwareScope,
    private val builder: KtSymbolByFirBuilder,
    final override val token: ValidityToken
) : KtScope {
    private val allNamesCached by cached {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    override fun getAllPossibleNames(): Set<Name> = allNamesCached

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        firScope.getCallableNames()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        firScope.getClassifierNames()
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        firScope.getConstructors(builder)
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getAllPossibleNames()
    }

}

internal fun FirScope.getCallableSymbols(callableNames: Collection<Name>, builder: KtSymbolByFirBuilder) = sequence {
    callableNames.forEach { name ->
        val callables = mutableListOf<KtCallableSymbol>()
        processFunctionsByName(name) { firSymbol ->
            callables.add(builder.functionLikeBuilder.buildFunctionSymbol(firSymbol))
        }
        processPropertiesByName(name) { firSymbol ->
            val symbol = when {
                firSymbol is FirPropertySymbol && firSymbol.fir.isSubstitutionOverride -> {
                    builder.variableLikeBuilder.buildVariableSymbol(firSymbol)
                }
                else -> builder.callableBuilder.buildCallableSymbol(firSymbol)
            }
            callables.add(symbol)
        }
        yieldAll(callables)
    }
}

internal fun FirScope.getClassifierSymbols(classLikeNames: Collection<Name>, builder: KtSymbolByFirBuilder): Sequence<KtClassifierSymbol> =
    sequence {
        classLikeNames.forEach { name ->
            val classifierSymbols = mutableListOf<KtClassifierSymbol>()
            processClassifiersByName(name) { firSymbol ->
                classifierSymbols.add(builder.classifierBuilder.buildClassifierSymbol(firSymbol))
            }
            yieldAll(classifierSymbols)
        }
    }

internal fun FirScope.getConstructors(builder: KtSymbolByFirBuilder): Sequence<KtConstructorSymbol> =
    sequence {
        val constructorSymbols = mutableListOf<KtConstructorSymbol>()
        processDeclaredConstructors { firSymbol ->
            constructorSymbols.add(builder.functionLikeBuilder.buildConstructorSymbol(firSymbol))
        }
        yieldAll(constructorSymbols)
    }
