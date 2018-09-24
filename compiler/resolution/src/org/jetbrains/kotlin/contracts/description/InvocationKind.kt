/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description

import kotlin.math.max
import kotlin.math.min

enum class InvocationKind(private val left: Int, private val right: Int) {
    ZERO(0, 0),          // 0..0
    AT_MOST_ONCE(0, 1),  // 0..1
    EXACTLY_ONCE(1, 1),  // 1..1
    AT_LEAST_ONCE(1, 2), // 1..*
    UNKNOWN(0, 2);       // 0..*

    companion object {
        private fun fromRange(left: Int, right: Int): InvocationKind = when (min(left, 1) to min(right, 2)) {
            0 to 0 -> ZERO
            0 to 1 -> AT_MOST_ONCE
            1 to 1 -> EXACTLY_ONCE
            1 to 2 -> AT_LEAST_ONCE
            0 to 2 -> UNKNOWN
            else -> throw IllegalArgumentException()
        }

        fun or(x: InvocationKind, y: InvocationKind): InvocationKind = fromRange(min(x.left, y.left), max(x.right, y.right))

        fun and(x: InvocationKind, y: InvocationKind): InvocationKind = fromRange(x.left + y.left, x.right + y.right)
    }

}

fun InvocationKind.isDefinitelyVisited(): Boolean = this == InvocationKind.EXACTLY_ONCE || this == InvocationKind.AT_LEAST_ONCE
fun InvocationKind.canBeRevisited(): Boolean = this == InvocationKind.UNKNOWN || this == InvocationKind.AT_LEAST_ONCE

