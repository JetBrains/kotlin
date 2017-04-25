/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.resolve.calls.model.ResolvedKotlinCall
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.model.ResolvedLambdaArgument
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import java.util.*


class MutableVariableWithConstraints(
        override val typeVariable: NewTypeVariable,
        constraints: Collection<Constraint> = emptyList()
) : VariableWithConstraints {
    override val constraints: List<Constraint> get() = mutableConstraints
    private val mutableConstraints = MyArrayList(constraints)

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
        return actualConstraint
    }

    fun removeLastConstraints(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.removeLast(shouldRemove)
    }

    // todo optimize it!
    fun removeConstrains(shouldRemove: (Constraint) -> Boolean) {
        val newConstraints = mutableConstraints.filter { !shouldRemove(it) }
        mutableConstraints.clear()
        mutableConstraints.addAll(newConstraints)
    }

    private fun newConstraintIsUseless(oldKind: ConstraintKind, newKind: ConstraintKind) =
            when (oldKind) {
                ConstraintKind.EQUALITY -> true
                ConstraintKind.LOWER -> newKind == ConstraintKind.LOWER
                ConstraintKind.UPPER -> newKind == ConstraintKind.UPPER
            }

    private class MyArrayList<E>(c: Collection<E>): ArrayList<E>(c) {
        fun removeLast(predicate: (E) -> Boolean) {
            val newSize = indexOfLast { !predicate(it) } + 1

            if (newSize != size) {
                removeRange(newSize, size)
            }
        }
    }

    override fun toString(): String {
        return "Constraints for $typeVariable"
    }

}


class MutableConstraintStorage : ConstraintStorage {
    override val allTypeVariables: MutableMap<TypeConstructor, NewTypeVariable> = LinkedHashMap()
    override val notFixedTypeVariables: MutableMap<TypeConstructor, MutableVariableWithConstraints> = LinkedHashMap()
    override val initialConstraints: MutableList<InitialConstraint> = ArrayList()
    override var maxTypeDepthFromInitialConstraints: Int = 1
    override val errors: MutableList<KotlinCallDiagnostic> = ArrayList()
    override val fixedTypeVariables: MutableMap<TypeConstructor, UnwrappedType> = LinkedHashMap()
    override val lambdaArguments: MutableList<ResolvedLambdaArgument> = ArrayList()
    override val innerCalls: MutableList<ResolvedKotlinCall.OnlyResolvedKotlinCall> = ArrayList()
}