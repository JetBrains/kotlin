/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.inference.constraintPosition

import java.util.HashMap
import org.jetbrains.jet.lang.resolve.calls.inference.constraintPosition.ConstraintPositionKind.*

public enum class ConstraintPositionKind {
    RECEIVER_POSITION
    EXPECTED_TYPE_POSITION
    VALUE_PARAMETER_POSITION
    TYPE_BOUND_POSITION
    COMPOUND_CONSTRAINT_POSITION
    FROM_COMPLETER
    SPECIAL

    public fun position(): ConstraintPosition {
        assert(this in setOf(RECEIVER_POSITION, EXPECTED_TYPE_POSITION, FROM_COMPLETER, SPECIAL))
        return ConstraintPositionImpl(this)
    }

    public fun position(index: Int): ConstraintPosition {
        assert(this in setOf(VALUE_PARAMETER_POSITION, TYPE_BOUND_POSITION))
        return ConstraintPositionWithIndex(this, index)
    }
}

public trait ConstraintPosition {
    val kind: ConstraintPositionKind

    fun isStrong(): Boolean = kind != TYPE_BOUND_POSITION
}

private open data class ConstraintPositionImpl(override val kind: ConstraintPositionKind) : ConstraintPosition {
    override fun toString() = "$kind"
}
private data class ConstraintPositionWithIndex(override val kind: ConstraintPositionKind, val index: Int) : ConstraintPosition {
    override fun toString() = "$kind($index)"
}

class CompoundConstraintPosition(
        val positions: Collection<ConstraintPosition>
) : ConstraintPositionImpl(ConstraintPositionKind.COMPOUND_CONSTRAINT_POSITION) {

    override fun isStrong() = positions.any { it.isStrong() }

    override fun toString() = "$kind(${positions.joinToString()}"
}

public fun getCompoundConstraintPosition(vararg positions: ConstraintPosition): ConstraintPosition {
    return CompoundConstraintPosition(positions.toList())
}