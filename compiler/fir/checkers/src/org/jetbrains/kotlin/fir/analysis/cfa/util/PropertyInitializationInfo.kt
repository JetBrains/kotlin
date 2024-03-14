/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.MarkedEventOccurrencesRange
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

typealias EventOccurrencesRangeAtNode = MarkedEventOccurrencesRange<CFGNode<*>>

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE") // K2 warning suppression, TODO: KT-62472
abstract class EventOccurrencesRangeInfo<E : EventOccurrencesRangeInfo<E, K>, K : Any>(
    map: PersistentMap<K, EventOccurrencesRangeAtNode> = persistentMapOf()
) : ControlFlowInfo<E, K, EventOccurrencesRangeAtNode>(map) {

    override fun merge(other: E, node: CFGNode<*>): E {
        @Suppress("UNCHECKED_CAST")
        var result = this as E

        val isUnion = node.isUnion
        if (isUnion) {
            // Special case: union with nothing is a no-op. Note that this is not the case
            // for non-union nodes, where all lower bounds on the non-empty side should become 0.
            if (result.isEmpty()) return other
            if (other.isEmpty()) return result
        }

        for (symbol in keys.union(other.keys)) {
            val kind1 = this[symbol] ?: MarkedEventOccurrencesRange.Zero
            val kind2 = other[symbol] ?: MarkedEventOccurrencesRange.Zero
            val newKind = if (kind1.location != null && kind1 == kind2) {
                // If ranges are equal and have the same location, the event happened before branching:
                //   <x>; if (p) { ... } else { ... }
                //   ExactlyOnce(x) ---> ExactlyOnce(x) ---> ExactlyOnce(x)
                //                   \-> ExactlyOnce(x) -/
                kind1
            } else if (isUnion) {
                when {
                    kind1 == MarkedEventOccurrencesRange.Zero -> kind2
                    kind2 == MarkedEventOccurrencesRange.Zero -> kind1
                    // Otherwise the event happens more than once (in different locations):
                    //   callBothFunctions({ <x> }, { <y> })
                    //   Zero ---> ExactlyOnce(x) ---> MoreThanOnce
                    //         \-> ExactlyOnce(y) -/
                    // Sum of two non-zero ranges cannot be `ExactlyOnce` or `AtMostOnce`. It should also not be possible
                    // to get a union of `ExactlyOnce` and `AtMostOnce` for the same location (in which case the correct
                    // result would be `ExactlyOnce`...probably...if it made any sense in the first place).
                    else -> (kind1.withoutMarker + kind2.withoutMarker).at(null)
                }
            } else {
                val newLocation = when {
                    kind1 == MarkedEventOccurrencesRange.Zero -> kind2.location
                    kind2 == MarkedEventOccurrencesRange.Zero -> kind1.location
                    // For a non-union node, there can be cases where we merge different kinds for the same location:
                    //   try { A; <x>; B } catch (e: Exception) { }
                    //   Zero ---> A ---> ExactlyOnce(x) ---> B -----[success]----------> AtMostOnce(x)
                    //          \--------\-----------------\-[catch]-> AtMostOnce(x) -/
                    // But most of the time the locations are different, in which case `node.fir` is a FIR parent of both:
                    //   if (p) { <x> } else { <y> }
                    //   Zero ---> ExactlyOnce(x) ---> ExactlyOnce(node [references FIR for the entire `if`])
                    //         \-> ExactlyOnce(y) -/
                    else -> kind1.location.takeIf { it == kind2.location } ?: node
                }
                (kind1.withoutMarker or kind2.withoutMarker).at(newLocation)
            }
            result = result.put(symbol, newKind)
        }
        return result
    }
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE") // K2 warning suppression, TODO: KT-62472
class PropertyInitializationInfo(
    map: PersistentMap<FirPropertySymbol, EventOccurrencesRangeAtNode> = persistentMapOf()
) : EventOccurrencesRangeInfo<PropertyInitializationInfo, FirPropertySymbol>(map) {
    companion object {
        val EMPTY = PropertyInitializationInfo()
    }

    override val constructor: (PersistentMap<FirPropertySymbol, EventOccurrencesRangeAtNode>) -> PropertyInitializationInfo =
        ::PropertyInitializationInfo
}

typealias PathAwarePropertyInitializationInfo = PathAwareControlFlowInfo<PropertyInitializationInfo>
