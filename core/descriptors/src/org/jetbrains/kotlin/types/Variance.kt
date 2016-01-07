/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
