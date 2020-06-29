/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass

internal abstract class KtFirDelegatingScope(private val builder: KtSymbolByFirBuilder) : KtScope {
    abstract val firScope: FirScope

    private var allNamesCached: Set<Name>? = null

    override fun getAllNames(): Set<Name> = withValidityAssertion {
        if (allNamesCached == null) {
            allNamesCached = firScope.getCallableNames() + firScope.getClassifierNames()
        }
        allNamesCached!!
    }

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

    override fun getCallableSymbols(): Sequence<KtSymbol> = withValidityAssertion {
        sequence {
            firScope.getCallableNames().forEach { name ->
                val callables = mutableListOf<KtSymbol>()
                firScope.processFunctionsByName(name) { firSymbol ->
                    (firSymbol.fir as? FirSimpleFunction)?.let { fir ->
                        callables.add(builder.buildFunctionSymbol(fir))
                    }
                }
                firScope.processPropertiesByName(name) { firSymbol ->
                    callables.add(builder.buildSymbol(firSymbol.fir))
                }
                yieldAll(callables)
            }
        }
    }

    override fun getClassClassLikeSymbols(): Sequence<KtClassLikeSymbol> = withValidityAssertion {
        sequence {
            firScope.getClassifierNames().forEach { name ->
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
}