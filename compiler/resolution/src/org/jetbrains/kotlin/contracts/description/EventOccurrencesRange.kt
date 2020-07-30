/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description

import kotlin.math.max
import kotlin.math.min

enum class EventOccurrencesRange(private val left: Int, private val right: Int) {
    ZERO(0, 0),          // 0..0
    AT_MOST_ONCE(0, 1),  // 0..1
    EXACTLY_ONCE(1, 1),  // 1..1
    AT_LEAST_ONCE(1, 3), // 1..*
    MORE_THAN_ONCE(2, 3), // 2..*
    UNKNOWN(0, 3);       // 0..*

    companion object {
        private fun fromRange(left: Int, right: Int): EventOccurrencesRange = when (min(left, 2) to min(right, 3)) {
            0 to 0 -> ZERO
            0 to 1 -> AT_MOST_ONCE
            0 to 2 -> UNKNOWN
            0 to 3 -> UNKNOWN
            1 to 1 -> EXACTLY_ONCE
            1 to 2 -> AT_LEAST_ONCE
            1 to 3 -> AT_LEAST_ONCE
            2 to 2 -> MORE_THAN_ONCE
            2 to 3 -> MORE_THAN_ONCE
            3 to 3 -> MORE_THAN_ONCE
            else -> throw IllegalArgumentException()
        }

        fun or(x: EventOccurrencesRange, y: EventOccurrencesRange): EventOccurrencesRange = fromRange(min(x.left, y.left), max(x.right, y.right))

        fun plus(x: EventOccurrencesRange, y: EventOccurrencesRange): EventOccurrencesRange = fromRange(x.left + y.left, x.right + y.right)
    }

    infix fun or(other: EventOccurrencesRange): EventOccurrencesRange = Companion.or(this, other)
    operator fun plus(other: EventOccurrencesRange): EventOccurrencesRange = Companion.plus(this, other)
    operator fun contains(other: EventOccurrencesRange): Boolean = left <= other.left && other.right <= right
}

fun EventOccurrencesRange.isDefinitelyVisited(): Boolean = this == EventOccurrencesRange.EXACTLY_ONCE || this == EventOccurrencesRange.AT_LEAST_ONCE || this == EventOccurrencesRange.MORE_THAN_ONCE
fun EventOccurrencesRange.canBeRevisited(): Boolean = this == EventOccurrencesRange.UNKNOWN || this == EventOccurrencesRange.AT_LEAST_ONCE || this == EventOccurrencesRange.MORE_THAN_ONCE
