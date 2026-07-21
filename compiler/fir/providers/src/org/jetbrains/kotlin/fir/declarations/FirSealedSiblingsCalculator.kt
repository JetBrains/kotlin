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
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.isJavaNonAbstractSealed
import org.jetbrains.kotlin.fir.resolve.getSuperTypes
import org.jetbrains.kotlin.fir.resolve.isSubclassOf
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.StandardClassIds

@ThreadSafeMutableState
class FirSealedSiblingsCalculator(private val session: FirSession) : FirSessionComponent {
    private typealias CachedSubclasses = Any

    private inline fun <T> CachedSubclasses.mapCachedSubclasses(
        onSingle: (FirClassSymbol<*>) -> T,
        onMultiple: (Set<FirClassSymbol<*>>) -> T,
    ): T = when (this) {
        is FirClassSymbol<*> -> onSingle(this)
        else -> @Suppress("UNCHECKED_CAST") onMultiple(this as Set<FirClassSymbol<*>>)
    }

    private inline fun CachedSubclasses.forEachCachedSubclass(action: (FirClassSymbol<*>) -> Unit) =
        mapCachedSubclasses(onSingle = action, onMultiple = { it.forEach(action) })

    private fun CachedSubclasses.mapTo(mutableSet: MutableSet<FirClassSymbol<*>>): Boolean =
        mapCachedSubclasses(mutableSet::add, mutableSet::addAll)

    private fun CachedSubclasses.toSet(): Set<FirClassSymbol<*>> =
        mapCachedSubclasses(onSingle = ::setOf, onMultiple = { it })

    private val allSubclassesCache: FirCache<FirClassSymbol<*>, CachedSubclasses, MutableSet<FirClassSymbol<*>>?> =
        session.firCachesFactory.createCache { symbol, visited ->
            when {
                visited != null && !visited.add(symbol) -> symbol
                symbol !is FirRegularClassSymbol -> symbol
                symbol.fir.modality == Modality.SEALED -> buildSet {
                    if (symbol.fir.isJavaNonAbstractSealed == true) {
                        add(symbol)
                    }

                    symbol.fir.getSealedClassInheritors(session).forEach {
                        val inheritor = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol ?: return@forEach
                        allSubclassesCache.getValue(inheritor, visited ?: mutableSetOf(symbol)).mapTo(this)
                    }
                }
                else -> symbol
            }
        }

    fun collectAllSubclassesFor(symbol: FirClassSymbol<*>): Set<FirClassSymbol<*>> =
        allSubclassesCache.getValue(symbol, context = null).toSet()

    private fun FirClassSymbol<*>.isSubclassOf(other: FirClassSymbol<*>): Boolean =
        isSubclassOf(other.toLookupTag(), session, isStrict = false, lookupInterfaces = true)

    private fun areUnrelated(a: FirClassSymbol<*>, b: FirClassSymbol<*>): Boolean =
        !a.isSubclassOf(b) && !b.isSubclassOf(a)

    private fun FirRegularClassSymbol.getImmediateSuperTypes(session: FirSession): List<ConeClassLikeType> =
        getSuperTypes(session, recursive = false)

    private object EmptyCachedSealedUniverse
    private typealias CachedSealedUniverse = Any

    private inline fun <T> CachedSealedUniverse.mapCachedUniverse(
        onEmpty: () -> T,
        onNonEmpty: (LinkedHashSet<FirClassSymbol<*>>) -> T,
    ): T = when {
        this != EmptyCachedSealedUniverse -> @Suppress("UNCHECKED_CAST") onNonEmpty(this as LinkedHashSet<FirClassSymbol<*>>)
        else -> onEmpty()
    }

    private inline fun CachedSealedUniverse.filterCachedUniverseTo(
        destination: MutableSet<FirClassSymbol<*>>,
        predicate: (FirClassSymbol<*>) -> Boolean,
    ) = mapCachedUniverse(
        onEmpty = { destination },
        onNonEmpty = { it.filterTo(destination, predicate) },
    )

    private inline fun CachedSealedUniverse.forEachSealedInCachedUniverse(action: (FirClassSymbol<*>) -> Unit) =
        mapCachedUniverse(onEmpty = {}, onNonEmpty = { it.forEach(action) })

    private fun CachedSealedUniverse.toMutableSet(): LinkedHashSet<FirClassSymbol<*>> =
        mapCachedUniverse(onEmpty = ::LinkedHashSet, onNonEmpty = { it })

    /**
     * Maps the given [FirRegularClassSymbol] to the set of [FirClassSymbol]s that coexist
     * in the same `sealed` hierarchy, and are not its superclasses.
     *
     * For instance, in the following hierarchy:
     * - `f(C) = setOf(D, F, G, I)`,
     * - `f(H) = setOf(C, D, F, I)`,
     * - `f(J) = setOf(C, D, F, I)`, and
     * - `f(A) = emptySet()`.
     * ```
     * sealed interface A {
     *     sealed interface B : A {
     *         interface C : B
     *         interface D : B
     *     }
     *
     *     sealed interface E : A {
     *         interface F : E
     *     }
     *
     *     interface G : A {
     *         interface H : G {
     *             interface I : H, E
     *             interface J : H
     *         }
     *     }
     * }
     * ```
     */
    private val relevantSealedUniverseCache: FirCache<FirRegularClassSymbol, CachedSealedUniverse, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            var result: CachedSealedUniverse = EmptyCachedSealedUniverse

            fun addIfNonSuper(other: FirClassSymbol<*>) = when {
                !symbol.isSubclassOf(other) -> result = result.toMutableSet().apply { add(other) }
                else -> {}
            }

            for (it in symbol.getImmediateSuperTypes(session)) {
                val symbol = it.toRegularClassSymbol(session)?.takeIf { it.classId != StandardClassIds.Any } ?: continue
                relevantSealedUniverseCache.getValue(symbol, null).forEachSealedInCachedUniverse(::addIfNonSuper)
                allSubclassesCache.getValue(symbol, context = null).forEachCachedSubclass(::addIfNonSuper)
            }

            result
        }

    fun collectComplementarySymbolsFor(symbol: FirRegularClassSymbol): Set<FirClassSymbol<*>> =
        relevantSealedUniverseCache.getValue(symbol, null).filterCachedUniverseTo(mutableSetOf()) {
            (symbol.isFinal || it.isFinal || symbol.isClass && it.isClass) && areUnrelated(symbol, it)
        }
}

val FirSession.sealedSiblingsCalculator: FirSealedSiblingsCalculator by FirSession.sessionComponentAccessor()

fun FirClassSymbol<*>.collectAllSubclasses(session: FirSession): Set<FirClassSymbol<*>> =
    session.sealedSiblingsCalculator.collectAllSubclassesFor(this)
