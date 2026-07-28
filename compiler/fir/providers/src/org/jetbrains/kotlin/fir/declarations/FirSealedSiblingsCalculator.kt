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

/**
 * When you implement a cache, and the same trivial value appears frequently, it's often best
 * to allocate a dedicated marker instance for it to avoid unnecessary allocations.
 * Furthermore, it's best if the value is of the same type so that no unchecked casts
 * happen at runtime as they come with a performance penalty.
 *
 * [ValueWrapper] abstracts away the type of the special value, hiding it from the cache
 * itself so that it doesn't accidentally modify it inappropriately.
 */
abstract class ValueWrapper<T, Value> {
    abstract val specialValue: Value

    inline fun <K> map(value: Value, onSpecial: () -> K, onRegular: (T) -> K): K = when {
        value === specialValue -> onSpecial()
        else -> onRegular(@OptIn(DangerousUnwrapping::class) unwrap(value))
    }

    abstract fun wrap(it: T): Value

    @DangerousUnwrapping
    abstract fun unwrap(value: Value): T

    @RequiresOptIn("Make sure `unwrap()` is never called over the special value.")
    annotation class DangerousUnwrapping

    class Refl<T>(override val specialValue: T) : ValueWrapper<T, T>() {
        override fun wrap(it: T): T = it

        @OptIn(DangerousUnwrapping::class)
        override fun unwrap(value: T): T = value
    }
}

inline fun <T, K> ValueWrapper<out Iterable<T>, K>.forEach(value: K, action: (T) -> Unit): Unit =
    map(value = value, onSpecial = {}, onRegular = { it.forEach(action) })

private class AllSubclassesCache<Value>(
    private val session: FirSession,
    private val wrapper: ValueWrapper<Set<FirClassSymbol<*>>, Value>,
) {
    inline fun <K> Value.map(onSingle: () -> K, onMultiple: (Set<FirClassSymbol<*>>) -> K): K =
        wrapper.map(value = this, onSpecial = onSingle, onRegular = onMultiple)

    inline fun FirClassSymbol<*>.forEachCachedSubclass(
        visited: LinkedHashSet<FirClassSymbol<*>>?,
        action: (FirClassSymbol<*>) -> Unit,
    ): Unit = wrapper.forEach(value = cache.getValue(this, visited), action = action)

    val cache: FirCache<FirClassSymbol<*>, Value, LinkedHashSet<FirClassSymbol<*>>?> =
        session.firCachesFactory.createCache { symbol, visited ->
            when {
                visited != null && !visited.add(symbol) -> wrapper.specialValue
                symbol !is FirRegularClassSymbol -> wrapper.specialValue
                symbol.fir.modality == Modality.SEALED -> buildSet {
                    if (symbol.fir.isJavaNonAbstractSealed == true) {
                        add(symbol)
                    }

                    symbol.fir.getSealedClassInheritors(session).forEach { it ->
                        val inheritor = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol ?: return@forEach
                        inheritor.forEachCachedSubclass(visited ?: linkedSetOf(symbol), ::add)
                    }
                }.let(wrapper::wrap)
                else -> wrapper.specialValue
            }
        }
}

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
private class RelevantSealedUniverseCache<Value>(
    private val session: FirSession,
    private val wrapper: ValueWrapper<LinkedHashSet<FirClassSymbol<*>>, Value>,
    private val allSubclassesCache: AllSubclassesCache<*>,
) {
    fun FirClassSymbol<*>.isSubclassOf(other: FirClassSymbol<*>): Boolean =
        isSubclassOf(other.toLookupTag(), session, isStrict = false, lookupInterfaces = true)

    private fun FirRegularClassSymbol.getImmediateSuperTypes(): List<ConeClassLikeType> =
        getSuperTypes(session, recursive = false)

    inline fun Value.filter(predicate: (FirClassSymbol<*>) -> Boolean): Set<FirClassSymbol<*>> =
        wrapper.map(value = this, onSpecial = ::emptySet, onRegular = { it.filterTo(mutableSetOf(), predicate) })

    private inline fun Value.forEach(action: (FirClassSymbol<*>) -> Unit): Unit =
        wrapper.forEach(value = this, action = action)

    private fun Value.withAdded(symbol: FirClassSymbol<*>): Value =
        wrapper.map(value = this, onSpecial = ::linkedSetOf, onRegular = { it })
            .also { it.add(symbol) }
            .let(wrapper::wrap)

    val cache: FirCache<FirRegularClassSymbol, Value, Nothing?> =
        session.firCachesFactory.createCache { symbol, _ ->
            var result: Value = wrapper.specialValue

            fun addIfNonSuper(other: FirClassSymbol<*>) = when {
                !symbol.isSubclassOf(other) -> result = result.withAdded(other)
                else -> {}
            }

            for (it in symbol.getImmediateSuperTypes()) {
                val symbol = it.toRegularClassSymbol(session)?.takeIf { it.classId != StandardClassIds.Any } ?: continue
                cache.getValue(symbol, null).forEach(::addIfNonSuper)
                with(allSubclassesCache) { symbol.forEachCachedSubclass(null, ::addIfNonSuper) }
            }

            result
        }
}

@ThreadSafeMutableState
class FirSealedSiblingsCalculator(private val session: FirSession) : FirSessionComponent {
    private val allSubclassesCache: AllSubclassesCache<*> = AllSubclassesCache(
        session = session,
        wrapper = ValueWrapper.Refl(emptySet()),
    )

    fun collectAllSubclassesOfSealed(symbol: FirClassSymbol<*>): Set<FirClassSymbol<*>> =
        with(allSubclassesCache) {
            cache.getValue(symbol, context = null).map(onSingle = ::setOf, onMultiple = { it })
        }

    private val relevantSealedUniverseCache: RelevantSealedUniverseCache<*> = RelevantSealedUniverseCache(
        session = session,
        wrapper = ValueWrapper.Refl(linkedSetOf()),
        allSubclassesCache = allSubclassesCache,
    )

    fun collectSealedSiblingsFor(symbol: FirRegularClassSymbol): Set<FirClassSymbol<*>> =
        with(relevantSealedUniverseCache) {
            cache.getValue(symbol, null).filter {
                (symbol.isFinal || it.isFinal || symbol.isClass && it.isClass) && !it.isSubclassOf(symbol)
            }
        }
}

val FirSession.sealedSiblingsCalculator: FirSealedSiblingsCalculator by FirSession.sessionComponentAccessor()

fun FirClassSymbol<*>.collectAllSubclassesIfSealed(session: FirSession): Set<FirClassSymbol<*>> =
    session.sealedSiblingsCalculator.collectAllSubclassesOfSealed(this)
