/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.isJavaNonAbstractSealed
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

interface FirComplementarySymbolsCalculator : FirSessionComponent {
    fun collectAllSubclassesFor(symbol: FirClassSymbol<*>, session: FirSession): Set<FirClassSymbol<*>>
}

class FirDefaultComplementarySymbolsCalculator(private val session: FirSession) : FirComplementarySymbolsCalculator {
    @JvmInline
    private value class CachedSubclasses private constructor(private val data: Any) {
        constructor(symbol: FirClassSymbol<*>) : this(data = symbol)
        constructor(symbols: Set<FirClassSymbol<*>>) : this(data = symbols)

        @Suppress("UNCHECKED_CAST")
        fun mapTo(mutableSet: MutableSet<FirClassSymbol<*>>) = when (data) {
            !is Set<*> -> mutableSet.add(data as FirClassSymbol<*>)
            else -> mutableSet.addAll(data as Set<FirClassSymbol<*>>)
        }
    }

    private val allSubclassesCache: FirCache<FirClassSymbol<*>, CachedSubclasses, MutableSet<FirClassSymbol<*>>> =
        session.firCachesFactory.createCache { symbol, visited ->
            when {
                !visited.add(symbol) -> CachedSubclasses(emptySet())
                symbol !is FirRegularClassSymbol -> CachedSubclasses(symbol)
                symbol.fir.modality == Modality.SEALED -> buildSet {
                    if (symbol.fir.isJavaNonAbstractSealed == true) {
                        add(symbol)
                    }

                    symbol.fir.getSealedClassInheritors(session).forEach {
                        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol ?: return@forEach
                        allSubclassesCache.getValue(symbol, visited).mapTo(this)
                    }
                }.let(::CachedSubclasses)
                else -> CachedSubclasses(symbol)
            }
        }

    override fun collectAllSubclassesFor(symbol: FirClassSymbol<*>, session: FirSession): Set<FirClassSymbol<*>> =
        buildSet { allSubclassesCache.getValue(symbol, mutableSetOf()).mapTo(this) }
}

val FirSession.complementarySymbolsCalculator: FirComplementarySymbolsCalculator by FirSession.sessionComponentAccessor()

fun FirClassSymbol<*>.collectAllSubclasses(session: FirSession): Set<FirClassSymbol<*>> =
    session.complementarySymbolsCalculator.collectAllSubclassesFor(this, session)
