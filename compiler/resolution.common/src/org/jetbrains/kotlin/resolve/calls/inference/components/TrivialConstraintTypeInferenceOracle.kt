/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate

class TrivialConstraintTypeInferenceOracle private constructor(context: TypeSystemInferenceExtensionContext) :
    TypeSystemInferenceExtensionContext by context {
    // This constructor is used for injection only in old FE
    constructor(context: TypeSystemInferenceExtensionContextDelegate) : this(context as TypeSystemInferenceExtensionContext)

    // The idea is to add knowledge that constraint `Nothing(?) <: T` is quite useless and
    // it's totally fine to go and resolve postponed argument without fixation T to Nothing(?).
    // In other words, constraint `Nothing(?) <: T` is *not* proper
    fun isNotInterestingConstraint(constraint: Constraint): Boolean {
        return constraint.kind == ConstraintKind.LOWER && constraint.type.typeConstructor().isNothingConstructor()
    }

    // This function controls the choice between sub and super result type
    // Even that Nothing(?) is the most specific type for subtype, it doesn't bring valuable information to the user,
    // therefore it is discriminated in favor of supertype
    fun isSuitableResultedType(
        resultType: KotlinTypeMarker
    ): Boolean {
        return !resultType.typeConstructor().isNothingConstructor() || (isK2 && resultType.isDynamic())
    }

    // It's possible to generate Nothing-like constraints inside incorporation mechanism:
    // For instance, when two type variables are in subtyping relation `T <: K`, after incorporation
    // there will be constraint `approximation(out K) <: K` => `Nothing <: K`, which is innocent
    // but can change result of the constraint system.
    // Therefore, here we avoid adding such trivial constraints to have stable constraint system
    fun isGeneratedConstraintTrivial(
        baseConstraint: Constraint,
        otherConstraint: Constraint,
        generatedConstraintType: KotlinTypeMarker,
        isSubtype: Boolean
    ): Boolean {
        if (isSubtype && (generatedConstraintType.isNothing() || generatedConstraintType.isFlexibleNothing())) return true
        if (!isSubtype && generatedConstraintType.isNullableAny()) return true

        // If types from constraints that will be used to generate new constraint already contains `Nothing(?)`,
        // then we can't decide that resulting constraint will be useless
        if (baseConstraint.type.contains { it.isNothingOrNullableNothing() }) return false
        if (otherConstraint.type.contains { it.isNothingOrNullableNothing() }) return false

        // It's important to preserve constraints with nullable Nothing: `Nothing? <: T` (see implicitNothingConstraintFromReturn.kt test)
        if (generatedConstraintType.containsOnlyNonNullableNothing()) return true

        return false
    }


    private fun KotlinTypeMarker.isNothingOrNullableNothing(): Boolean =
        typeConstructor().isNothingConstructor()


    private fun KotlinTypeMarker.containsOnlyNonNullableNothing(): Boolean =
        contains {
            (it.isNothing() || it.isFlexibleNothing()) &&
                    !(it is SimpleTypeMarker && it.typeConstructor().isNothingConstructor() && it.isMarkedNullable())
        }


    companion object {
        fun create(context: TypeSystemInferenceExtensionContext) = TrivialConstraintTypeInferenceOracle(context)
    }
}
