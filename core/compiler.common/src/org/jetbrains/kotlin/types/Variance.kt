/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

enum class Variance(
        val label: String,
        val allowsInPosition: Boolean,
        val allowsOutPosition: Boolean,
        private val superpositionFactor: Int
) {
    INVARIANT("", true, true, 0),
    IN_VARIANCE("in", true, false, -1),
    OUT_VARIANCE("out", false, true, +1);

    fun allowsPosition(position: Variance): Boolean
            = when (position) {
                IN_VARIANCE -> allowsInPosition
                OUT_VARIANCE -> allowsOutPosition
                INVARIANT -> allowsInPosition && allowsOutPosition
            }

    fun superpose(other: Variance): Variance {
        val r = this.superpositionFactor * other.superpositionFactor
        return when (r) {
            0 -> INVARIANT
            -1 -> IN_VARIANCE
            +1 -> OUT_VARIANCE
            else -> throw IllegalStateException("Illegal factor: $r")
        }
    }

    fun opposite(): Variance {
        return when (this) {
            INVARIANT -> INVARIANT
            IN_VARIANCE -> OUT_VARIANCE
            OUT_VARIANCE -> IN_VARIANCE
        }
    }

    override fun toString() = label
}
