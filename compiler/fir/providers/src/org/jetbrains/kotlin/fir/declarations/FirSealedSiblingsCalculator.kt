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
    private val singleValueCachedSubclass = emptySet<FirClassSymbol<*>>()
    private typealias CachedSubclasses = Set<FirClassSymbol<*>>

    private inline fun <T> FirClassSymbol<*>.mapCachedSubclasses(
        visited: LinkedHashSet<FirClassSymbol<*>>?,
        onSingle: (FirClassSymbol<*>) -> T,
        onMultiple: (Set<FirClassSymbol<*>>) -> T,
    ): T {
        val cached = allSubclassesCache.getValue(this, visited)

        return when {
            cached === singleValueCachedSubclass -> onSingle(this)
            else -> onMultiple(cached)
        }
    }

    private inline fun FirClassSymbol<*>.forEachCachedSubclass(
        visited: LinkedHashSet<FirClassSymbol<*>>?,
        action: (FirClassSymbol<*>) -> Unit,
    ): Unit = mapCachedSubclasses(visited, onSingle = action, onMultiple = { it.forEach(action) })

    private val allSubclassesCache: FirCache<FirClassSymbol<*>, CachedSubclasses, LinkedHashSet<FirClassSymbol<*>>?> =
        session.firCachesFactory.createCacheWithSuggestedLimits { symbol, visited ->
            when {
                symbol !is FirRegularClassSymbol -> singleValueCachedSubclass
                symbol.fir.modality == Modality.SEALED -> buildSet {
                    if (symbol.fir.isJavaNonAbstractSealed == true) {
                        add(symbol)
                    }

                    symbol.fir.getSealedClassInheritors(session).forEach { it ->
                        val inheritor = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol ?: return@forEach
                        if (visited != null && !visited.add(inheritor)) return@forEach
                        inheritor.forEachCachedSubclass(visited ?: linkedSetOf(symbol, inheritor), ::add)
                    }
                }
                else -> singleValueCachedSubclass
            }
        }

    fun collectAllSubclassesOfSealed(symbol: FirClassSymbol<*>): Set<FirClassSymbol<*>> =
        symbol.mapCachedSubclasses(visited = null, onSingle = ::setOf, onMultiple = { it })

    private fun FirClassSymbol<*>.isSubclassOf(other: FirClassSymbol<*>): Boolean =
        isSubclassOf(other.toLookupTag(), session, isStrict = false, lookupInterfaces = true)

    private fun FirRegularClassSymbol.getImmediateSuperTypes(): List<ConeClassLikeType> =
        getSuperTypes(session, recursive = false)

    private val emptyCachedSealedUniverse = linkedSetOf<FirClassSymbol<*>>()
    private typealias CachedSealedUniverse = LinkedHashSet<FirClassSymbol<*>>

    private inline fun <T> CachedSealedUniverse.mapCachedUniverse(
        onEmpty: () -> T,
        onNonEmpty: (LinkedHashSet<FirClassSymbol<*>>) -> T,
    ): T = when {
        this !== emptyCachedSealedUniverse -> onNonEmpty(this)
        else -> onEmpty()
    }

    private inline fun CachedSealedUniverse.filterCachedUniverse(
        predicate: (FirClassSymbol<*>) -> Boolean,
    ): Set<FirClassSymbol<*>> = mapCachedUniverse(
        onEmpty = { emptyCachedSealedUniverse },
        onNonEmpty = { it.filterTo(mutableSetOf(), predicate) },
    )

    private inline fun CachedSealedUniverse.forEachInCachedUniverse(action: (FirClassSymbol<*>) -> Unit): Unit =
        mapCachedUniverse(onEmpty = {}, onNonEmpty = { it.forEach(action) })

    private fun CachedSealedUniverse.cachedUniverseToMutableSet(): CachedSealedUniverse =
        mapCachedUniverse(onEmpty = ::linkedSetOf, onNonEmpty = { it })

    /**
     * Maps the given [FirRegularClassSymbol] to the set of [FirClassSymbol]s that coexist
     * in the same `sealed` hierarchy as its superclasses but are not these superclasses themselves.
     *
     * Consider the following examples.
     * ```
     * sealed interface A {          // f(A) = emptySet()
     *     sealed interface B : A    // f(B) = setOf(C)
     *     sealed interface C : A
     * }
     * ```
     * ```
     * sealed interface A {
     *     interface B : A {
     *         interface C : B       // f(C) = setOf(F)
     *     }
     *
     *     sealed interface E : A {
     *         interface F : E       // f(F) = setOf(B)
     *     }
     * }
     * ```
     * ```
     * sealed interface A {
     *     sealed interface E : A
     *
     *     interface G : A {
     *         interface H : G {     // f(H) = setOf(I)
     *             interface I : H, E
     *         }
     *     }
     * }
     * ```
     * ```
     * sealed interface A
     * sealed interface B : A        // f(B) = emptySet()
     * ```
     * ```
     * sealed interface I            // f(I) = emptySet()
     *
     * sealed class A                // f(A) = emptySet()
     * class B : A                   // f(B) = setOf(C)
     * class C : A, I                // f(C) = setOf(B, D)
     *
     * class D : I                   // f(D) = setOf(C)
     * ```
     */
    private val relevantSealedUniverseCache: FirCache<FirRegularClassSymbol, CachedSealedUniverse, Nothing?> =
        session.firCachesFactory.createCacheWithSuggestedLimits { symbol, _ ->
            var result: CachedSealedUniverse = emptyCachedSealedUniverse

            fun addIfNonSuper(other: FirClassSymbol<*>) = when {
                !symbol.isSubclassOf(other) -> result = result.cachedUniverseToMutableSet().apply { add(other) }
                else -> {}
            }

            for (it in symbol.getImmediateSuperTypes()) {
                val symbol = it.toRegularClassSymbol(session)?.takeIf { it.classId != StandardClassIds.Any } ?: continue
                relevantSealedUniverseCache.getValue(symbol, null).forEachInCachedUniverse(::addIfNonSuper)
                symbol.forEachCachedSubclass(null, ::addIfNonSuper)
            }

            result
        }

    fun collectSealedSiblingsFor(symbol: FirRegularClassSymbol): Set<FirClassSymbol<*>> =
        relevantSealedUniverseCache.getValue(symbol, null).filterCachedUniverse {
            (symbol.isFinal || it.isFinal || symbol.isClass && it.isClass) && !it.isSubclassOf(symbol)
        }
}

val FirSession.sealedSiblingsCalculator: FirSealedSiblingsCalculator by FirSession.sessionComponentAccessor()

fun FirClassSymbol<*>.collectAllSubclassesIfSealed(session: FirSession): Set<FirClassSymbol<*>> =
    session.sealedSiblingsCalculator.collectAllSubclassesOfSealed(this)
