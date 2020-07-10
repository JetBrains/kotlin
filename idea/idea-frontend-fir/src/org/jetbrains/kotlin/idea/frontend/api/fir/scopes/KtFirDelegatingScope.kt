/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.PossiblyFirFakeOverrideSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal abstract class KtFirDelegatingScope(private val builder: KtSymbolByFirBuilder) : KtScope, ValidityOwnerByValidityToken {
    abstract val firScope: FirScope

    private val allNamesCached by cached { getCallableNames() + getClassLikeSymbolNames() }

    override fun getAllNames(): Set<Name> = allNamesCached

    override fun getCallableNames(): Set<Name> = withValidityAssertion {
        firScope.getCallableNames()
    }

    override fun getClassLikeSymbolNames(): Set<Name> = withValidityAssertion {
        firScope.getClassifierNames()
    }

    override fun getAllSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        sequence {
            yieldAll(getCallableSymbols())
            yieldAll(getClassClassLikeSymbols())
        }
    }

    override fun getCallableSymbols(): Sequence<KtCallableSymbol> = withValidityAssertion {
        sequence {
            getCallableNames().forEach { name ->
                val callables = mutableListOf<KtCallableSymbol>()
                firScope.processFunctionsByName(name) { firSymbol ->
                    (firSymbol.fir as? FirSimpleFunction)?.let { fir ->
                        callables.add(builder.buildFunctionSymbol(fir, firSymbol.realDeclarationOrigin()))
                    }
                }
                firScope.processPropertiesByName(name) { firSymbol ->
                    val symbol = when {
                        firSymbol is FirPropertySymbol && firSymbol.isFakeOverride -> {
                            builder.buildVariableSymbol(firSymbol.fir, firSymbol.realDeclarationOrigin())
                        }
                        else -> builder.buildCallableSymbol(firSymbol.fir)
                    }
                    callables.add(symbol)
                }
                yieldAll(callables)
            }
        }
    }

    override fun getClassClassLikeSymbols(): Sequence<KtClassLikeSymbol> = withValidityAssertion {
        sequence {
            getClassLikeSymbolNames().forEach { name ->
                val classLikeSymbols = mutableListOf<KtClassLikeSymbol>()
                firScope.processClassifiersByName(name) { firSymbol ->
                    (firSymbol.fir as? FirClassLikeDeclaration<*>)?.let {
                        classLikeSymbols.add(builder.buildClassLikeSymbol(it))
                    }
                }
                yieldAll(classLikeSymbols)
            }
        }
    }

    override fun containsName(name: Name): Boolean = withValidityAssertion {
        name in getAllNames()
    }

    companion object {
        private fun FirBasedSymbol<*>.realDeclarationOrigin(): FirDeclarationOrigin? {
            if (this !is PossiblyFirFakeOverrideSymbol<*, *> || !isFakeOverride) return null
            var current: FirBasedSymbol<*>? = this.overriddenSymbol
            while (current is PossiblyFirFakeOverrideSymbol<*, *> && current.isFakeOverride) {
                current = current.overriddenSymbol
            }
            return (current?.fir as? FirDeclaration)?.origin
        }
    }
}