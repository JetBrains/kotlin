/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.inference.trimToSize
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.typeUtil.unCapture
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


class MutableVariableWithConstraints(
    override val typeVariable: TypeVariableMarker,
    constraints: Collection<Constraint> = emptyList()
) : VariableWithConstraints {
    override val constraints: List<Constraint>
        get() {
            if (simplifiedConstraints == null) {
                simplifiedConstraints = simplifyConstraints()
            }
            return simplifiedConstraints!!
        }

    // see @OnlyInputTypes annotation
    val projectedInputCallTypes: Collection<UnwrappedType>
        get() =
            mutableConstraints.filter {
                val position = it.position.from
                position is ArgumentConstraintPosition || position is ReceiverConstraintPosition || position is ExpectedTypeConstraintPosition
            }.map {
                (it.type as KotlinType).unCapture().unwrap()
            }

    private val mutableConstraints = ArrayList(constraints)

    private var simplifiedConstraints: List<Constraint>? = null

    // return new actual constraint, if this constraint is new
    fun addConstraint(constraint: Constraint): Constraint? {
        val previousConstraintWithSameType = constraints.filter { it.typeHashCode == constraint.typeHashCode && it.type == constraint.type }

        if (previousConstraintWithSameType.any { previous -> newConstraintIsUseless(previous, constraint) })
            return null

        val addAsEqualityConstraint = previousConstraintWithSameType.any { previous ->
            when (previous.kind) {
                ConstraintKind.LOWER -> constraint.kind.isUpper()
                ConstraintKind.UPPER -> constraint.kind.isLower()
                ConstraintKind.EQUALITY -> true
            }
        }

        val actualConstraint = if (addAsEqualityConstraint)
            Constraint(
                ConstraintKind.EQUALITY,
                constraint.type,
                constraint.position,
                constraint.typeHashCode,
                derivedFrom = constraint.derivedFrom
            )
        else
            constraint

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

    private fun newConstraintIsUseless(old: Constraint, new: Constraint): Boolean {
        // Constraints from declared upper bound are quite special -- they aren't considered as a proper ones
        // In other words, user-defined constraints have "higher" priority and here we're trying not to loose them
        if (old.position.from is DeclaredUpperBoundConstraintPosition && new.position.from !is DeclaredUpperBoundConstraintPosition)
            return false

        return when (old.kind) {
            ConstraintKind.EQUALITY -> true
            ConstraintKind.LOWER -> new.kind.isLower()
            ConstraintKind.UPPER -> new.kind.isUpper()
        }
    }

    private fun simplifyConstraints(): List<Constraint> {
        val distinctConstraints = removeDuplicatesFromDeclaredUpperBoundConstraints(mutableConstraints)

        val equalityConstraints = distinctConstraints
            .filter { it.kind == ConstraintKind.EQUALITY }
            .groupBy { it.typeHashCode }
        return distinctConstraints.filter { isUsefulConstraint(it, equalityConstraints) }
    }

    private fun removeDuplicatesFromDeclaredUpperBoundConstraints(constraints: List<Constraint>): MutableList<Constraint> {
        val currentConstraints = constraints.toMutableList()
        val iterator = currentConstraints.iterator()
        while (iterator.hasNext()) {
            val potentialDuplicate = iterator.next()

            if (potentialDuplicate.position.from !is DeclaredUpperBoundConstraintPosition) continue
            val hasDuplicate = currentConstraints.any { other ->
                potentialDuplicate !== other &&
                        potentialDuplicate.typeHashCode == other.typeHashCode &&
                        potentialDuplicate.type == other.type &&
                        potentialDuplicate.kind == other.kind
            }

            if (hasDuplicate)
                iterator.remove()
        }

        return currentConstraints
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
    override val allTypeVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker> = LinkedHashMap()
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints> = LinkedHashMap()
    override val initialConstraints: MutableList<InitialConstraint> = ArrayList()
    override var maxTypeDepthFromInitialConstraints: Int = 1
    override val errors: MutableList<KotlinCallDiagnostic> = ArrayList()
    override val hasContradiction: Boolean get() = errors.any { !it.candidateApplicability.isSuccess }
    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, KotlinTypeMarker> = LinkedHashMap()
    override val postponedTypeVariables: ArrayList<TypeVariableMarker> = ArrayList()
}
