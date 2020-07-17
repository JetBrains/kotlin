/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.inference.trimToSize
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.typeUtil.unCapture
import org.jetbrains.kotlin.utils.SmartList


class MutableVariableWithConstraints private constructor(
    override val typeVariable: TypeVariableMarker,
    constraints: List<Constraint>? // assume simplified and deduplicated
) : VariableWithConstraints {

    constructor(typeVariable: TypeVariableMarker) : this(typeVariable, null)

    constructor(other: VariableWithConstraints) : this(other.typeVariable, other.constraints)

    override val constraints: List<Constraint>
        get() {
            if (simplifiedConstraints == null) {
                simplifiedConstraints = mutableConstraints.simplifyConstraints()
            }
            return simplifiedConstraints!!
        }

    // see @OnlyInputTypes annotation
    val projectedInputCallTypes: Collection<UnwrappedType>
        get() = mutableConstraints
            .mapNotNullTo(SmartList()) {
                if (it.position.from is OnlyInputTypeConstraintPosition || it.inputTypePositionBeforeIncorporation != null)
                    (it.type as KotlinType).unCapture().unwrap()
                else null
            }

    private val mutableConstraints = if (constraints == null) SmartList() else SmartList(constraints)

    private var simplifiedConstraints: SmartList<Constraint>? = mutableConstraints

    // return new actual constraint, if this constraint is new
    fun addConstraint(constraint: Constraint): Constraint? {

        for (previousConstraint in constraints) {
            if (previousConstraint.typeHashCode == constraint.typeHashCode
                && previousConstraint.type == constraint.type
                && previousConstraint.isNullabilityConstraint == constraint.isNullabilityConstraint
            ) {
                if (newConstraintIsUseless(previousConstraint, constraint)) return null
                val isMatchingForSimplification = when (previousConstraint.kind) {
                    ConstraintKind.LOWER -> constraint.kind.isUpper()
                    ConstraintKind.UPPER -> constraint.kind.isLower()
                    ConstraintKind.EQUALITY -> true
                }
                if (isMatchingForSimplification) {
                    val actualConstraint = Constraint(
                        ConstraintKind.EQUALITY,
                        constraint.type,
                        constraint.position,
                        constraint.typeHashCode,
                        derivedFrom = constraint.derivedFrom,
                        isNullabilityConstraint = false
                    )
                    mutableConstraints.add(actualConstraint)
                    simplifiedConstraints = null
                    return actualConstraint
                }
            }
        }

        mutableConstraints.add(constraint)
        if (simplifiedConstraints != null && simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints!!.add(constraint)
        }
        return constraint
    }

    // This method should be used only for transaction in constraint system
    // shouldRemove should give true only for tail elements
    internal fun removeLastConstraints(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.trimToSize(mutableConstraints.indexOfLast { !shouldRemove(it) } + 1)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
    }

    // This method should be used only when constraint system has state COMPLETION
    internal fun removeConstrains(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.removeAll(shouldRemove)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
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

    private fun SmartList<Constraint>.simplifyConstraints(): SmartList<Constraint> {
        val equalityConstraints =
            filter { it.kind == ConstraintKind.EQUALITY }
                .groupBy { it.typeHashCode }
        return when {
            equalityConstraints.isEmpty() -> this
            else -> filterTo(SmartList()) { isUsefulConstraint(it, equalityConstraints) }
        }
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
    override val initialConstraints: MutableList<InitialConstraint> = SmartList()
    override var maxTypeDepthFromInitialConstraints: Int = 1
    override val errors: MutableList<KotlinCallDiagnostic> = SmartList()
    override val hasContradiction: Boolean get() = errors.any { !it.candidateApplicability.isSuccess }
    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, KotlinTypeMarker> = LinkedHashMap()
    override val postponedTypeVariables: MutableList<TypeVariableMarker> = SmartList()
}
