/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.model

import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.substitute
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

/**
 * Every type variable can be in the following states:
 *  - not fixed => there is several constraints for this type variable(possible no one).
 *      for this type variable we have VariableWithConstraints in map notFixedTypeVariables
 *  - fixed to proper type or not proper type. For such type variable there is no VariableWithConstraints in notFixedTypeVariables.
 *      Also we should guaranty that there is no other constraints in other VariableWithConstraints which depends on this fixed type variable.
 *
 *  Note: fixedTypeVariables can contains a proper and not proper type.
 *
 *  Fixing procedure(to proper types). First of all we should determinate fixing order.
 *  After it, for every type variable we do the following:
 *  - determinate result proper type
 *  - add equality constraint, for example: T = Int
 *  - run incorporation and generate all new constraints
 *  - after is we remove VariableWithConstraints for type variable T from map notFixedTypeVariables
 *  - also we remove all constraint in other variable which contains T
 *  - add result type to fixedTypeVariables.
 *
 *  Note fixing procedure to not proper type the same. The only difference in determination result type.
 *
 */

interface ConstraintStorage {
    val allTypeVariables: Map<TypeConstructor, NewTypeVariable>
    val notFixedTypeVariables: Map<TypeConstructor, VariableWithConstraints>
    val initialConstraints: List<InitialConstraint>
    val maxTypeDepthFromInitialConstraints: Int
    val errors: List<KotlinCallDiagnostic>
    val hasContradiction: Boolean
    val fixedTypeVariables: Map<TypeConstructor, UnwrappedType>
    val postponedTypeVariables: List<NewTypeVariable>

    object Empty : ConstraintStorage {
        override val allTypeVariables: Map<TypeConstructor, NewTypeVariable> get() = emptyMap()
        override val notFixedTypeVariables: Map<TypeConstructor, VariableWithConstraints> get() = emptyMap()
        override val initialConstraints: List<InitialConstraint> get() = emptyList()
        override val maxTypeDepthFromInitialConstraints: Int get() = 1
        override val errors: List<KotlinCallDiagnostic> get() = emptyList()
        override val hasContradiction: Boolean get() = false
        override val fixedTypeVariables: Map<TypeConstructor, UnwrappedType> get() = emptyMap()
        override val postponedTypeVariables: List<NewTypeVariable> get() = emptyList()
    }
}

enum class ConstraintKind {
    LOWER,
    UPPER,
    EQUALITY;

    fun isLower(): Boolean = this == LOWER
    fun isUpper(): Boolean = this == UPPER
    fun isEqual(): Boolean = this == EQUALITY
}

class Constraint(
    val kind: ConstraintKind,
    val type: UnwrappedType, // flexible types here is allowed
    val position: IncorporationConstraintPosition,
    val typeHashCode: Int = type.hashCode()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Constraint

        if (typeHashCode != other.typeHashCode) return false
        if (kind != other.kind) return false
        if (position != other.position) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode() = typeHashCode

    override fun toString() = "$kind($type) from $position"
}

interface VariableWithConstraints {
    val typeVariable: NewTypeVariable
    val constraints: List<Constraint>
}

class InitialConstraint(
    val a: UnwrappedType,
    val b: UnwrappedType,
    val constraintKind: ConstraintKind, // see [checkConstraint]
    val position: ConstraintPosition
) {
    override fun toString(): String {
        val sign =
            when (constraintKind) {
                ConstraintKind.EQUALITY -> "=="
                ConstraintKind.LOWER -> ":>"
                ConstraintKind.UPPER -> "<:"
            }
        return "$a $sign $b from $position"
    }
}

fun InitialConstraint.checkConstraint(substitutor: TypeSubstitutor): Boolean {
    val newA = substitutor.substitute(a)
    val newB = substitutor.substitute(a)
    return checkConstraint(newB, constraintKind, newA)
}

fun checkConstraint(constraintType: UnwrappedType, constraintKind: ConstraintKind, resultType: UnwrappedType): Boolean {
    val typeChecker = KotlinTypeChecker.DEFAULT
    return when (constraintKind) {
        ConstraintKind.EQUALITY -> typeChecker.equalTypes(constraintType, resultType)
        ConstraintKind.LOWER -> typeChecker.isSubtypeOf(constraintType, resultType)
        ConstraintKind.UPPER -> typeChecker.isSubtypeOf(resultType, constraintType)
    }
}