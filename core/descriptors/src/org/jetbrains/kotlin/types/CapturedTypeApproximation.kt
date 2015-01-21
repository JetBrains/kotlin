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

package org.jetbrains.kotlin.types.typesApproximation

import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.Variance
import java.util.ArrayList
import org.jetbrains.kotlin.types.JetTypeImpl
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.resolve.calls.inference.CapturedTypeConstructor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.LazyType
import org.jetbrains.kotlin.resolve.calls.inference.isCaptured

public data class ApproximationBounds<T>(
        public val lower: T,
        public val upper: T
)

private class TypeArgument(
        val typeParameter: TypeParameterDescriptor,
        val inProjection: JetType,
        val outProjection: JetType
) {
    val isConsistent: Boolean
        get() = JetTypeChecker.DEFAULT.isSubtypeOf(inProjection, outProjection)
}

private val NULLABLE_ANY = KotlinBuiltIns.getInstance().getNullableAnyType()

private val NOTHING = KotlinBuiltIns.getInstance().getNothingType()

private fun TypeArgument.toTypeProjection(): TypeProjection {
    assert(isConsistent) { "Only consistent enhanced type propection can be converted to type projection" }
    fun removeProjectionIfRedundant(variance: Variance) = if (variance == typeParameter.getVariance()) Variance.INVARIANT else variance
    return when {
        inProjection == outProjection -> TypeProjectionImpl(inProjection)
        inProjection == NOTHING && typeParameter.getVariance() != Variance.IN_VARIANCE ->
            TypeProjectionImpl(removeProjectionIfRedundant(Variance.OUT_VARIANCE), outProjection)
        outProjection == NULLABLE_ANY -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.IN_VARIANCE), inProjection)
        else -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.OUT_VARIANCE), outProjection)
    }
}

private fun TypeProjection.toTypeArgument(typeParameter: TypeParameterDescriptor) =
        when (TypeSubstitutor.combine(typeParameter.getVariance(), getProjectionKind()) : Variance) {
            Variance.INVARIANT -> TypeArgument(typeParameter, getType(), getType())
            Variance.IN_VARIANCE -> TypeArgument(typeParameter, getType(), NULLABLE_ANY)
            Variance.OUT_VARIANCE -> TypeArgument(typeParameter, NOTHING, getType())
        }

public fun approximateCapturedTypesIfNecessary(typeProjection: TypeProjection?): TypeProjection? {
    if (typeProjection == null) return null

    val type = typeProjection.getType()
    if (!TypeUtils.containsSpecialType(type, { it.isCaptured() })) {
        return typeProjection
    }
    val howThisTypeIsUsed = typeProjection.getProjectionKind()
    if (howThisTypeIsUsed == Variance.OUT_VARIANCE) {
        // only 'return' type containing captured types should be over-approximated
        val approximation = approximateCapturedTypes(type)
        return TypeProjectionImpl(howThisTypeIsUsed, approximation.upper)
    }
    return substituteCapturedTypes(typeProjection)
}

private fun substituteCapturedTypes(typeProjection: TypeProjection): TypeProjection? {
    val typeSubstitutor = TypeSubstitutor.create(object : TypeSubstitution() {
        override fun get(typeConstructor: TypeConstructor?): TypeProjection? {
            return (typeConstructor as? CapturedTypeConstructor)?.typeProjection
        }
    })
    return typeSubstitutor.substituteWithoutApproximation(typeProjection)
}

public fun approximateCapturedTypes(type: JetType): ApproximationBounds<JetType> {
    val typeConstructor = type.getConstructor()
    if (type.isCaptured()) {
        val typeProjection = (typeConstructor as CapturedTypeConstructor).typeProjection
        // todo: preserve flexibility as well
        fun JetType.makeNullableIfNeeded() = TypeUtils.makeNullableIfNeeded(this, type.isMarkedNullable())
        val bound = typeProjection.getType().makeNullableIfNeeded()

        return when (typeProjection.getProjectionKind()) {
            Variance.IN_VARIANCE -> ApproximationBounds(bound, NULLABLE_ANY)
            Variance.OUT_VARIANCE -> ApproximationBounds(NOTHING.makeNullableIfNeeded(), bound)
            else -> throw AssertionError("Only nontrivial projections should have been captured, not: $typeProjection")
        }
    }
    if (type.getArguments().isEmpty()) {
        return ApproximationBounds(type, type)
    }
    val lowerBoundArguments = ArrayList<TypeArgument>()
    val upperBoundArguments = ArrayList<TypeArgument>()
    for ((typeProjection, typeParameter) in type.getArguments().zip(typeConstructor.getParameters())) {
        val (lower, upper) = approximateProjection(typeProjection.toTypeArgument(typeParameter))
        lowerBoundArguments.add(lower)
        upperBoundArguments.add(upper)
    }
    val lowerBoundIsTrivial = lowerBoundArguments.any { !it.isConsistent }
    return ApproximationBounds(
            if (lowerBoundIsTrivial) NOTHING else type.replaceTypeArguments(lowerBoundArguments),
            type.replaceTypeArguments(upperBoundArguments))
}

private fun JetType.replaceTypeArguments(newTypeArguments: List<TypeArgument>): JetType {
    assert(getArguments().size() == newTypeArguments.size()) { "Incorrect type arguments $newTypeArguments" }
    return JetTypeImpl(getAnnotations(), getConstructor(), isMarkedNullable(), newTypeArguments.map { it.toTypeProjection() }, getMemberScope())
}

private fun approximateProjection(typeArgument: TypeArgument): ApproximationBounds<TypeArgument> {
    val (inLower, inUpper) = approximateCapturedTypes(typeArgument.inProjection)
    val (outLower, outUpper) = approximateCapturedTypes(typeArgument.outProjection)
    return ApproximationBounds(
            lower = TypeArgument(typeArgument.typeParameter, inUpper, outLower),
            upper = TypeArgument(typeArgument.typeParameter, inLower, outUpper))
}
