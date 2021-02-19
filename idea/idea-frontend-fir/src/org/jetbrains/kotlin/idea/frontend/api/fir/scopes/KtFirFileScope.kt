/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFileSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtDeclarationScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithDeclarations
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal class KtFirFileScope(
    override val owner: KtFirFileSymbol,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtDeclarationScope<KtSymbolWithDeclarations>,
    ValidityTokenOwner {

    private val allNamesCached by cached {
        _callableNames + _classifierNames
    }

    override fun getAllNames(): Set<Name> = allNamesCached

    private val _callableNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.firRef.withFir {
            it.declarations.mapNotNullTo(result) { firDeclaration ->
                when (firDeclaration) {
                    is FirSimpleFunction -> firDeclaration.name
                    is FirProperty -> firDeclaration.name
                    else -> null
                }
            }
        }
        result
    }

    override fun getCallableNames(): Set<Name> = _callableNames

    private val _classifierNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.firRef.withFir {
            it.declarations.mapNotNullTo(result) { firDeclaration ->
                (firDeclaration as? FirRegularClass)?.name
            }
        }
        result
    }

    override fun getClassifierNames(): Set<Name> = _classifierNames

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        owner.firRef.withFir {
            sequence {
                it.declarations.forEach { firDeclaration ->
                    val callableDeclaration = when (firDeclaration) {
                        is FirSimpleFunction -> firDeclaration.takeIf { nameFilter(firDeclaration.name) }
                        is FirProperty -> firDeclaration.takeIf { nameFilter(firDeclaration.name) }
                        else -> null
                    }

                    if (callableDeclaration != null) {
                        yield(builder.callableBuilder.buildCallableSymbol(callableDeclaration))
                    }
                }
            }
        }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        owner.firRef.withFir {
            sequence {
                it.declarations.forEach { firDeclaration ->
                    val classLikeDeclaration = (firDeclaration as? FirRegularClass)?.takeIf { klass -> nameFilter(klass.name) }
                    if (classLikeDeclaration != null) {
                        yield(builder.classifierBuilder.buildClassLikeSymbol(classLikeDeclaration))
                    }
                }
            }
        }
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = emptySequence()
}