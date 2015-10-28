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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.EQUAL
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.SUB_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.CompoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.Variance.INVARIANT
import org.jetbrains.kotlin.types.typeUtil.getNestedArguments
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import java.util.*

data class ConstraintContext(
        val position: ConstraintPosition,
        // see TypeBounds.Bound.derivedFrom
        val derivedFrom: Set<TypeParameterDescriptor>? = null,
        val initial: Boolean = false)

fun ConstraintSystemImpl.incorporateBound(newBound: Bound) {
    val typeVariable = newBound.typeVariable
    val typeBounds = getTypeBounds(typeVariable)

    for (oldBoundIndex in typeBounds.bounds.indices) {
        addConstraintFromBounds(typeBounds.bounds[oldBoundIndex], newBound)
    }
    val boundsUsedIn = getBoundsUsedIn(typeVariable)
    for (index in boundsUsedIn.indices) {
        val boundUsedIn = boundsUsedIn[index]
        generateNewBound(boundUsedIn, newBound)
    }

    val constrainingType = newBound.constrainingType
    if (isMyTypeVariable(constrainingType)) {
        val context = ConstraintContext(newBound.position, newBound.derivedFrom)
        addBound(getMyTypeVariable(constrainingType)!!, typeVariable.correspondingType, newBound.kind.reverse(), context)
        return
    }

    getNestedTypeVariables(constrainingType, original = true).forEach {
        val boundsForNestedVariable = getTypeBounds(it).bounds
        for (index in boundsForNestedVariable.indices) {
            generateNewBound(newBound, boundsForNestedVariable[index])
        }
    }
}

private fun ConstraintSystemImpl.addConstraintFromBounds(old: Bound, new: Bound) {
    if (old == new) return

    val oldType = old.constrainingType
    val newType = new.constrainingType
    val context = ConstraintContext(CompoundConstraintPosition(old.position, new.position), old.derivedFrom + new.derivedFrom)

    when {
        old.kind.ordinal() < new.kind.ordinal() -> addConstraint(SUB_TYPE, oldType, newType, context)
        old.kind.ordinal() > new.kind.ordinal() -> addConstraint(SUB_TYPE, newType, oldType, context)
        old.kind == new.kind && old.kind == EXACT_BOUND -> addConstraint(EQUAL, oldType, newType, context)
    }
}

private fun ConstraintSystemImpl.generateNewBound(bound: Bound, substitution: Bound) {
    // Let's have a bound 'T <=> My<R>', and a substitution 'R <=> Type'.
    // Here <=> means lower_bound, upper_bound or exact_bound constraint.
    // Then a new bound 'T <=> My<_/in/out Type>' can be generated.

    val substitutedType = when (substitution.kind) {
        EXACT_BOUND -> substitution.constrainingType
        UPPER_BOUND -> CapturedType(TypeProjectionImpl(Variance.OUT_VARIANCE, substitution.constrainingType))
        LOWER_BOUND -> CapturedType(TypeProjectionImpl(Variance.IN_VARIANCE, substitution.constrainingType))
    }

    val newTypeProjection = TypeProjectionImpl(substitutedType)
    val substitutor = TypeSubstitutor.create(mapOf(substitution.typeVariable.getTypeConstructor() to newTypeProjection))
    val type = substitutor.substitute(bound.constrainingType, INVARIANT) ?: return

    val position = CompoundConstraintPosition(bound.position, substitution.position)

    fun addNewBound(newConstrainingType: KotlinType, newBoundKind: BoundKind) {
        // We don't generate new recursive constraints
        val nestedTypeVariables = getNestedTypeVariables(newConstrainingType, original = false)
        if (nestedTypeVariables.contains(bound.typeVariable)) return

        // We don't generate constraint if a type variable was substituted twice
        val derivedFrom = HashSet(bound.derivedFrom + substitution.derivedFrom)
        if (derivedFrom.contains(substitution.typeVariable)) return

        derivedFrom.add(substitution.typeVariable)
        addBound(bound.typeVariable, newConstrainingType, newBoundKind, ConstraintContext(position, derivedFrom))
    }

    if (substitution.kind == EXACT_BOUND) {
        addNewBound(type, bound.kind)
        return
    }
    val approximationBounds = approximateCapturedTypes(type)
    // todo
    // if we allow non-trivial type projections, we bump into errors like
    // "Empty intersection for types [MutableCollection<in ('Int'..'Int?')>, MutableCollection<out Any?>, MutableCollection<in Int>]"
    fun KotlinType.containsConstrainingTypeWithoutProjection() = this.getNestedArguments().any {
        it.getType().getConstructor() == substitution.constrainingType.getConstructor() && it.getProjectionKind() == Variance.INVARIANT
    }
    if (approximationBounds.upper.containsConstrainingTypeWithoutProjection() && bound.kind != LOWER_BOUND) {
        addNewBound(approximationBounds.upper, UPPER_BOUND)
    }
    if (approximationBounds.lower.containsConstrainingTypeWithoutProjection() && bound.kind != UPPER_BOUND) {
        addNewBound(approximationBounds.lower, LOWER_BOUND)
    }
}