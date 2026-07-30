/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.utils.isClass
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.isJavaNonAbstractSealed
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

@ThreadSafeMutableState
class FirSealedSiblingsCalculator(private val session: FirSession) : FirSessionComponent {
    private val allSubclassesCache: FirCache<FirClassSymbol<*>, Set<FirClassSymbol<*>>, MutableSet<FirClassSymbol<*>>?> =
        session.firCachesFactory.createCache { symbol, visited ->
            when {
                visited != null && !visited.add(symbol) -> emptySet()
                symbol !is FirRegularClassSymbol -> setOf(symbol)
                symbol.fir.modality == Modality.SEALED -> buildSet {
                    if (symbol.fir.isJavaNonAbstractSealed == true) {
                        add(symbol)
                    }

                    symbol.fir.getSealedClassInheritors(session).forEach {
                        val inheritor = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol ?: return@forEach
                        addAll(allSubclassesCache.getValue(inheritor, visited ?: mutableSetOf(symbol)))
                    }
                }
                else -> setOf(symbol)
            }
        }

    fun collectAllSubclassesFor(symbol: FirClassSymbol<*>): Set<FirClassSymbol<*>> =
        allSubclassesCache.getValue(symbol, context = null)

    private fun FirClassSymbol<*>.isSubclassOf(other: FirClassSymbol<*>): Boolean =
        isSubclassOf(other.toLookupTag(), session, isStrict = false, lookupInterfaces = true)

    private fun areUnrelated(a: FirClassSymbol<*>, b: FirClassSymbol<*>): Boolean =
        !a.isSubclassOf(b) && !b.isSubclassOf(a)

    fun collectComplementarySymbolsFor(symbol: FirRegularClassSymbol): List<FirClassSymbol<*>> {
        val superTypes = symbol.getSuperTypes(session)
            .mapNotNullTo(mutableSetOf()) { it.toRegularClassSymbol(session) }

        return superTypes.flatMap { superType ->
            if (!superType.isSealed) return@flatMap emptyList()

            superType.fir.getSealedClassInheritors(session)
                .mapNotNull { it.toSymbol(session) as? FirRegularClassSymbol }
                .filter { (symbol.isFinal || it.isFinal || symbol.isClass && it.isClass) && areUnrelated(symbol, it) }
        }
    }
}

val FirSession.sealedSiblingsCalculator: FirSealedSiblingsCalculator by FirSession.sessionComponentAccessor()

fun FirClassSymbol<*>.collectAllSubclasses(session: FirSession): Set<FirClassSymbol<*>> =
    session.sealedSiblingsCalculator.collectAllSubclassesFor(this)
