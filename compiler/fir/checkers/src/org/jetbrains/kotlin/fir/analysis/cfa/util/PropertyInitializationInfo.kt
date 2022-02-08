/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.NormalPath
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

abstract class EventOccurrencesRangeInfo<E : EventOccurrencesRangeInfo<E, K>, K : Any>(
    map: PersistentMap<K, EventOccurrencesRange> = persistentMapOf()
) : ControlFlowInfo<E, K, EventOccurrencesRange>(map) {

    override fun merge(other: E): E =
        operation(other, EventOccurrencesRange::or)

    fun plus(other: E): E =
        when {
            isEmpty() -> other
            other.isEmpty() ->
                @Suppress("UNCHECKED_CAST")
                this as E
            else -> operation(other, EventOccurrencesRange::plus)
        }

    private inline fun operation(other: E, op: (EventOccurrencesRange, EventOccurrencesRange) -> EventOccurrencesRange): E {
        @Suppress("UNCHECKED_CAST")
        var result = this as E
        for (symbol in keys.union(other.keys)) {
            val kind1 = this[symbol] ?: EventOccurrencesRange.ZERO
            val kind2 = other[symbol] ?: EventOccurrencesRange.ZERO
            result = result.put(symbol, op.invoke(kind1, kind2))
        }
        return result
    }
}

class PropertyInitializationInfo(
    map: PersistentMap<FirPropertySymbol, EventOccurrencesRange> = persistentMapOf()
) : EventOccurrencesRangeInfo<PropertyInitializationInfo, FirPropertySymbol>(map) {
    companion object {
        val EMPTY = PropertyInitializationInfo()
    }

    override val constructor: (PersistentMap<FirPropertySymbol, EventOccurrencesRange>) -> PropertyInitializationInfo =
        ::PropertyInitializationInfo

    override val empty: () -> PropertyInitializationInfo =
        ::EMPTY
}

class PathAwarePropertyInitializationInfo(
    map: PersistentMap<EdgeLabel, PropertyInitializationInfo> = persistentMapOf()
) : PathAwareControlFlowInfo<PathAwarePropertyInitializationInfo, PropertyInitializationInfo>(map) {
    companion object {
        val EMPTY = PathAwarePropertyInitializationInfo(persistentMapOf(NormalPath to PropertyInitializationInfo.EMPTY))
    }

    override val constructor: (PersistentMap<EdgeLabel, PropertyInitializationInfo>) -> PathAwarePropertyInitializationInfo =
        ::PathAwarePropertyInitializationInfo

    override val empty: () -> PathAwarePropertyInitializationInfo =
        ::EMPTY
}
