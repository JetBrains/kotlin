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

package org.jetbrains.kotlin.resolve.calls.inference.constraintPosition

import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.*

public enum class ConstraintPositionKind {
    RECEIVER_POSITION,
    EXPECTED_TYPE_POSITION,
    VALUE_PARAMETER_POSITION,
    TYPE_BOUND_POSITION,
    COMPOUND_CONSTRAINT_POSITION,
    FROM_COMPLETER,
    SPECIAL;

    public fun position(): ConstraintPosition {
        assert(this in setOf(RECEIVER_POSITION, EXPECTED_TYPE_POSITION, FROM_COMPLETER, SPECIAL))
        return ConstraintPositionImpl(this)
    }

    public fun position(index: Int): ConstraintPosition {
        assert(this in setOf(VALUE_PARAMETER_POSITION, TYPE_BOUND_POSITION))
        return ConstraintPositionWithIndex(this, index)
    }
}

public interface ConstraintPosition {
    val kind: ConstraintPositionKind

    fun isStrong(): Boolean = kind != TYPE_BOUND_POSITION

    fun isParameter(): Boolean = kind in setOf(VALUE_PARAMETER_POSITION, RECEIVER_POSITION)
}

private data class ConstraintPositionImpl(override val kind: ConstraintPositionKind) : ConstraintPosition {
    override fun toString() = "$kind"
}

private data class ConstraintPositionWithIndex(override val kind: ConstraintPositionKind, val index: Int) : ConstraintPosition {
    override fun toString() = "$kind($index)"
}

class CompoundConstraintPosition(vararg positions: ConstraintPosition) : ConstraintPosition {

    override val kind: ConstraintPositionKind
        get() = COMPOUND_CONSTRAINT_POSITION

    val positions: Collection<ConstraintPosition> =
            positions.flatMap { if (it is CompoundConstraintPosition) it.positions else listOf(it) }.toSet()

    override fun isStrong() = positions.any { it.isStrong() }

    override fun toString() = "$kind(${positions.joinToString()})"
}

fun ConstraintPosition.derivedFrom(kind: ConstraintPositionKind): Boolean {
    return if (this !is CompoundConstraintPosition) this.kind == kind else positions.any { it.kind == kind }
}