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

package org.jetbrains.kotlin.types.typesApproximation

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.inference.CapturedTypeConstructor
import org.jetbrains.kotlin.resolve.calls.inference.isCaptured
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.builtIns
import java.util.*

data class ApproximationBounds<out T>(
        val lower: T,
        val upper: T
)

private class TypeArgument(
        val typeParameter: TypeParameterDescriptor,
        val inProjection: KotlinType,
        val outProjection: KotlinType
) {
    val isConsistent: Boolean
        get() = KotlinTypeChecker.DEFAULT.isSubtypeOf(inProjection, outProjection)
}

private fun TypeArgument.toTypeProjection(): TypeProjection {
    assert(isConsistent) {
        val descriptorRenderer = DescriptorRenderer.withOptions {
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
        }
        "Only consistent enhanced type projection can be converted to type projection, but " +
        "[${descriptorRenderer.render(typeParameter)}: <${descriptorRenderer.renderType(inProjection)}, ${descriptorRenderer.renderType(outProjection)}>]" +
        " was found"
    }
    fun removeProjectionIfRedundant(variance: Variance) = if (variance == typeParameter.variance) Variance.INVARIANT else variance
    return when {
        inProjection == outProjection -> TypeProjectionImpl(inProjection)
        KotlinBuiltIns.isNothing(inProjection) && typeParameter.variance != Variance.IN_VARIANCE ->
            TypeProjectionImpl(removeProjectionIfRedundant(Variance.OUT_VARIANCE), outProjection)
        KotlinBuiltIns.isNullableAny(outProjection) -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.IN_VARIANCE), inProjection)
        else -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.OUT_VARIANCE), outProjection)
    }
}

private fun TypeProjection.toTypeArgument(typeParameter: TypeParameterDescriptor) =
        when (TypeSubstitutor.combine(typeParameter.variance, this)) {
            Variance.INVARIANT -> TypeArgument(typeParameter, type, type)
            Variance.IN_VARIANCE -> TypeArgument(typeParameter, type, typeParameter.builtIns.nullableAnyType)
            Variance.OUT_VARIANCE -> TypeArgument(typeParameter, typeParameter.builtIns.nothingType, type)
        }

fun approximateCapturedTypesIfNecessary(typeProjection: TypeProjection?, approximateContravariant: Boolean): TypeProjection? {
    if (typeProjection == null) return null
    if (typeProjection.isStarProjection) return typeProjection

    val type = typeProjection.type
    if (!TypeUtils.contains(type, { it.isCaptured() })) {
        return typeProjection
    }
    val howThisTypeIsUsed = typeProjection.projectionKind
    if (howThisTypeIsUsed == Variance.OUT_VARIANCE) {
        // only 'return' type containing captured types should be over-approximated
        val approximation = approximateCapturedTypes(type)
        return TypeProjectionImpl(howThisTypeIsUsed, approximation.upper)
    }

    if (approximateContravariant) {
        // TODO: assert that howThisTypeIsUsed is always IN
        val approximation = approximateCapturedTypes(type).lower
        return TypeProjectionImpl(howThisTypeIsUsed, approximation)
    }

    return substituteCapturedTypesWithProjections(typeProjection)
}

private fun substituteCapturedTypesWithProjections(typeProjection: TypeProjection): TypeProjection? {
    val typeSubstitutor = TypeSubstitutor.create(object : TypeConstructorSubstitution() {
        override fun get(key: TypeConstructor): TypeProjection? {
            val capturedTypeConstructor = key as? CapturedTypeConstructor ?: return null
            if (capturedTypeConstructor.typeProjection.isStarProjection) {
                return TypeProjectionImpl(Variance.OUT_VARIANCE, capturedTypeConstructor.typeProjection.type)
            }
            return capturedTypeConstructor.typeProjection
        }
    })
    return typeSubstitutor.substituteWithoutApproximation(typeProjection)
}

// todo: dynamic & raw type?
fun approximateCapturedTypes(type: KotlinType): ApproximationBounds<KotlinType> {
    if (type.isFlexible()) {
        val boundsForFlexibleLower = approximateCapturedTypes(type.lowerIfFlexible())
        val boundsForFlexibleUpper = approximateCapturedTypes(type.upperIfFlexible())

        return ApproximationBounds(
                KotlinTypeFactory.flexibleType(boundsForFlexibleLower.lower.lowerIfFlexible(), boundsForFlexibleUpper.lower.upperIfFlexible()),
                KotlinTypeFactory.flexibleType(boundsForFlexibleLower.upper.lowerIfFlexible(), boundsForFlexibleUpper.upper.upperIfFlexible()))
    }

    val typeConstructor = type.constructor
    if (type.isCaptured()) {
        val typeProjection = (typeConstructor as CapturedTypeConstructor).typeProjection
        fun KotlinType.makeNullableIfNeeded() = TypeUtils.makeNullableIfNeeded(this, type.isMarkedNullable)
        val bound = typeProjection.type.makeNullableIfNeeded()

        return when (typeProjection.projectionKind) {
            Variance.IN_VARIANCE -> ApproximationBounds(bound, type.builtIns.nullableAnyType)
            Variance.OUT_VARIANCE -> ApproximationBounds(type.builtIns.nothingType.makeNullableIfNeeded(), bound)
            else -> throw AssertionError("Only nontrivial projections should have been captured, not: $typeProjection")
        }
    }
    if (type.arguments.isEmpty() || type.arguments.size != typeConstructor.parameters.size) {
        return ApproximationBounds(type, type)
    }
    val lowerBoundArguments = ArrayList<TypeArgument>()
    val upperBoundArguments = ArrayList<TypeArgument>()
    for ((typeProjection, typeParameter) in type.arguments.zip(typeConstructor.parameters)) {
        val typeArgument = typeProjection.toTypeArgument(typeParameter)

        // Protection from infinite recursion caused by star projection
        if (typeProjection.isStarProjection) {
            lowerBoundArguments.add(typeArgument)
            upperBoundArguments.add(typeArgument)
        }
        else {
            val (lower, upper) = approximateProjection(typeArgument)
            lowerBoundArguments.add(lower)
            upperBoundArguments.add(upper)
        }
    }
    val lowerBoundIsTrivial = lowerBoundArguments.any { !it.isConsistent }
    return ApproximationBounds(
            if (lowerBoundIsTrivial) type.builtIns.nothingType else type.replaceTypeArguments(lowerBoundArguments),
            type.replaceTypeArguments(upperBoundArguments))
}

private fun KotlinType.replaceTypeArguments(newTypeArguments: List<TypeArgument>): KotlinType {
    assert(arguments.size == newTypeArguments.size) { "Incorrect type arguments $newTypeArguments" }
    return replace(newTypeArguments.map { it.toTypeProjection() })
}

private fun approximateProjection(typeArgument: TypeArgument): ApproximationBounds<TypeArgument> {
    val (inLower, inUpper) = approximateCapturedTypes(typeArgument.inProjection)
    val (outLower, outUpper) = approximateCapturedTypes(typeArgument.outProjection)
    return ApproximationBounds(
            lower = TypeArgument(typeArgument.typeParameter, inUpper, outLower),
            upper = TypeArgument(typeArgument.typeParameter, inLower, outUpper))
}
