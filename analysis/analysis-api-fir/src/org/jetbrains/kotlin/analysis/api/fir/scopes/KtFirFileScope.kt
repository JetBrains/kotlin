/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.name.Name

internal class KtFirFileScope(
    private val owner: KtFirFileSymbol,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtScope {

    private val allNamesCached by cached {
        _callableNames + _classifierNames
    }

    override fun getAllPossibleNames(): Set<Name> = allNamesCached

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

    override fun getPossibleCallableNames(): Set<Name> = _callableNames

    private val _classifierNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.firSymbol.fir.declarations
            .mapNotNullTo(result) { firDeclaration ->
                (firDeclaration as? FirRegularClass)?.name
            }

        result
    }

    override fun getPossibleClassifierNames(): Set<Name> = _classifierNames

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

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        sequence {
            owner.firSymbol.fir.declarations.forEach { firDeclaration ->
                val classLikeDeclaration = when (firDeclaration) {
                        is FirTypeAlias -> if (nameFilter(firDeclaration.name)) firDeclaration else null
                        is FirRegularClass -> if (nameFilter(firDeclaration.name)) firDeclaration else null
                        else -> null
                    }
                if (classLikeDeclaration != null) {
                    yield(builder.classifierBuilder.buildClassLikeSymbol(classLikeDeclaration.symbol))
                }
            }
        }
    }


    override fun getConstructors(): Sequence<KtConstructorSymbol> = emptySequence()

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        emptySequence()
    }
}