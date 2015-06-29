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
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.EQUAL
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.SUB_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.Bound
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.EXACT_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.LOWER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.TypeBounds.BoundKind.UPPER_BOUND
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.CompoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.Variance.INVARIANT
import org.jetbrains.kotlin.types.Variance.IN_VARIANCE
import org.jetbrains.kotlin.types.typeUtil.getNestedTypeArguments
import java.util.ArrayList

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
        addBound(getMyTypeVariable(constrainingType)!!, typeVariable.correspondingType, newBound.kind.reverse(), newBound.position)
        return
    }
    constrainingType.getNestedTypeVariables().forEach {
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
    val position = CompoundConstraintPosition(old.position, new.position)

    when (old.kind to new.kind) {
        LOWER_BOUND to UPPER_BOUND, LOWER_BOUND to EXACT_BOUND, EXACT_BOUND to UPPER_BOUND ->
            addConstraint(SUB_TYPE, oldType, newType, position)

        UPPER_BOUND to LOWER_BOUND, UPPER_BOUND to EXACT_BOUND, EXACT_BOUND to LOWER_BOUND ->
            addConstraint(SUB_TYPE, newType, oldType, position)

        EXACT_BOUND to EXACT_BOUND ->
            addConstraint(EQUAL, oldType, newType, position)
    }
}

private fun ConstraintSystemImpl.generateNewBound(
        bound: Bound,
        substitution: Bound
) {
    // Let's have a variable T, a bound 'T <=> My<R>', and a substitution 'R <=> Type'.
    // Here <=> means lower_bound, upper_bound or exact_bound constraint.
    // Then a new bound 'T <=> My<Type>' can be generated.

    // A variance of R in 'My<R>' (with respect to both use-site and declaration-site variance).
    val substitutionVariance: Variance = bound.constrainingType.getNestedTypeArguments().firstOrNull {
        getMyTypeVariable(it.getType()) === substitution.typeVariable
    }?.getProjectionKind() ?: return

    // We don't substitute anything into recursive constraints
    if (substitution.typeVariable == bound.typeVariable) return

    //todo  variance checker
    val newKind = computeKindOfNewBound(bound.kind, substitutionVariance, substitution.kind) ?: return

    val newTypeProjection = TypeProjectionImpl(substitutionVariance, substitution.constrainingType)
    val substitutor = TypeSubstitutor.create(mapOf(substitution.typeVariable.getTypeConstructor() to newTypeProjection))
    val newConstrainingType = substitutor.substitute(bound.constrainingType, INVARIANT)!!

    // We don't generate new recursive constraints
    val nestedTypeVariables = newConstrainingType.getNestedTypeVariables()
    if (nestedTypeVariables.contains(bound.typeVariable) || nestedTypeVariables.contains(substitution.typeVariable)) return

    val position = CompoundConstraintPosition(bound.position, substitution.position)
    addBound(bound.typeVariable, newConstrainingType, newKind, position)
}

private fun computeKindOfNewBound(constrainingKind: BoundKind, substitutionVariance: Variance, substitutionKind: BoundKind): BoundKind? {
    // In examples below: List<out T>, MutableList<T>, Comparator<in T>, the variance of My<T> may be any.

    // T <=> My<R>, R <=> Type -> T <=> My<Type>

    // T < My<R>, R = Int -> T < My<Int>
    if (substitutionKind == EXACT_BOUND) return constrainingKind

    // T < MutableList<R>, R < Number - nothing can be inferred (R might become 'Int' later)
    // todo T < MutableList<R>, R < Int => T < MutableList<out Int>
    if (substitutionVariance == INVARIANT) return null

    val kind = if (substitutionVariance == IN_VARIANCE) substitutionKind.reverse() else substitutionKind

    // T = List<R>, R < Int -> T < List<Int>; T = Consumer<R>, R < Int -> T > Consumer<Int>
    if (constrainingKind == EXACT_BOUND) return kind

    // T < List<R>, R < Int -> T < List<Int>; T < Consumer<R>, R > Int -> T < Consumer<Int>
    if (constrainingKind == kind) return kind

    // otherwise we can generate no new constraints
    return null
}