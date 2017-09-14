/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.inference.trimToSize
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import java.util.*
import kotlin.collections.ArrayList


class MutableVariableWithConstraints(
        override val typeVariable: NewTypeVariable,
        constraints: Collection<Constraint> = emptyList()
) : VariableWithConstraints {
    override val constraints: List<Constraint> get() {
        if (simplifiedConstraints == null) {
            simplifiedConstraints = simplifyConstraints()
        }
        return simplifiedConstraints!!
    }
    private val mutableConstraints = ArrayList(constraints)

    private var simplifiedConstraints: List<Constraint>? = null

    // return new actual constraint, if this constraint is new
    fun addConstraint(constraint: Constraint): Constraint? {
        val previousConstraintWithSameType = constraints.filter { it.typeHashCode == constraint.typeHashCode && it.type == constraint.type }

        if (previousConstraintWithSameType.any { newConstraintIsUseless(it.kind, constraint.kind) }) {
            return null
        }

        val actualConstraint = if (previousConstraintWithSameType.isNotEmpty()) {
            // i.e. previous is LOWER and new is UPPER or opposite situation
            Constraint(ConstraintKind.EQUALITY, constraint.type, constraint.position, constraint.typeHashCode)
        }
        else {
            constraint
        }
        mutableConstraints.add(actualConstraint)
        simplifiedConstraints = null
        return actualConstraint
    }

    // This method should be used only for transaction in constraint system
    // shouldRemove should give true only for tail elements
    internal fun removeLastConstraints(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.trimToSize(mutableConstraints.indexOfLast { !shouldRemove(it) } + 1)
        simplifiedConstraints = null
    }

    // This method should be used only when constraint system has state COMPLETION
    internal fun removeConstrains(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.removeAll(shouldRemove)
        simplifiedConstraints = null
    }

    private fun newConstraintIsUseless(oldKind: ConstraintKind, newKind: ConstraintKind) =
            when (oldKind) {
                ConstraintKind.EQUALITY -> true
                ConstraintKind.LOWER -> newKind == ConstraintKind.LOWER
                ConstraintKind.UPPER -> newKind == ConstraintKind.UPPER
            }

    private fun simplifyConstraints(): List<Constraint> {
        val equalityConstraints = mutableConstraints
                .filter { it.kind == ConstraintKind.EQUALITY }
                .groupBy { it.typeHashCode }
        return mutableConstraints.filter { isUsefulConstraint(it, equalityConstraints) }
    }

    private fun isUsefulConstraint(constraint: Constraint, equalityConstraints: Map<Int, List<Constraint>>): Boolean {
        if (constraint.kind == ConstraintKind.EQUALITY) return true
        return equalityConstraints[constraint.typeHashCode]?.none { it.type == constraint.type } ?: true
    }

    override fun toString(): String {
        return "Constraints for $typeVariable"
    }
}


internal class MutableConstraintStorage : ConstraintStorage {
    override val allTypeVariables: MutableMap<TypeConstructor, NewTypeVariable> = LinkedHashMap()
    override val notFixedTypeVariables: MutableMap<TypeConstructor, MutableVariableWithConstraints> = LinkedHashMap()
    override val initialConstraints: MutableList<InitialConstraint> = ArrayList()
    override var maxTypeDepthFromInitialConstraints: Int = 1
    override val errors: MutableList<KotlinCallDiagnostic> = ArrayList()
    override val fixedTypeVariables: MutableMap<TypeConstructor, UnwrappedType> = LinkedHashMap()
}