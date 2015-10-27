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

@file:JvmName("FuzzyTypeUtils")

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.*
import java.util.*

fun CallableDescriptor.fuzzyReturnType(): FuzzyType? {
    val returnType = getReturnType() ?: return null
    return FuzzyType(returnType, getTypeParameters())
}

fun CallableDescriptor.fuzzyExtensionReceiverType(): FuzzyType? {
    val receiverParameter = getExtensionReceiverParameter()
    return if (receiverParameter != null) FuzzyType(receiverParameter.getType(), getTypeParameters()) else null
}

fun FuzzyType.makeNotNullable() = FuzzyType(type.makeNotNullable(), freeParameters)
fun FuzzyType.makeNullable() = FuzzyType(type.makeNullable(), freeParameters)
fun FuzzyType.nullability() = type.nullability()

fun FuzzyType.isAlmostEverything(): Boolean {
    if (freeParameters.isEmpty()) return false
    val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    if (typeParameter !in freeParameters) return false
    return typeParameter.upperBounds.singleOrNull()?.isAnyOrNullableAny() ?: false
}

class FuzzyType(
        val type: KotlinType,
        freeParameters: Collection<TypeParameterDescriptor>
) {
    public val freeParameters: Set<TypeParameterDescriptor>

    init {
        if (freeParameters.isNotEmpty()) {
            val usedTypeParameters = HashSet<TypeParameterDescriptor>()
            usedTypeParameters.addUsedTypeParameters(type)
            this.freeParameters = freeParameters.filter { it in usedTypeParameters }.toSet()
        }
        else {
            this.freeParameters = emptySet()
        }
    }

    override fun equals(other: Any?) = other is FuzzyType && other.type == type && other.freeParameters == freeParameters

    override fun hashCode() = type.hashCode()

    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: KotlinType) {
        val typeParameter = type.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            typeParameter.getLowerBounds().forEach { addUsedTypeParameters(it) }
            typeParameter.getUpperBounds().forEach { addUsedTypeParameters(it) }
        }

        for (argument in type.getArguments()) {
            if (!argument.isStarProjection) { // otherwise we can fall into infinite recursion
                addUsedTypeParameters(argument.getType())
            }
        }
    }

    public fun checkIsSubtypeOf(otherType: FuzzyType): TypeSubstitutor?
            = matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)

    public fun checkIsSuperTypeOf(otherType: FuzzyType): TypeSubstitutor?
            = matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)

    public fun checkIsSubtypeOf(otherType: KotlinType): TypeSubstitutor?
            = checkIsSubtypeOf(FuzzyType(otherType, emptyList()))

    public fun checkIsSuperTypeOf(otherType: KotlinType): TypeSubstitutor?
            = checkIsSuperTypeOf(FuzzyType(otherType, emptyList()))

    private enum class MatchKind {
        IS_SUBTYPE,
        IS_SUPERTYPE
    }

    private fun matchedSubstitutor(otherType: FuzzyType, matchKind: MatchKind): TypeSubstitutor? {
        if (type.isError()) return null
        if (otherType.type.isError()) return null

        fun KotlinType.checkInheritance(otherType: KotlinType): Boolean {
            return when (matchKind) {
                MatchKind.IS_SUBTYPE -> this.isSubtypeOf(otherType)
                MatchKind.IS_SUPERTYPE -> otherType.isSubtypeOf(this)
            }
        }

        if (freeParameters.isEmpty() && otherType.freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType.type)) TypeSubstitutor.EMPTY else null
        }

        val constraintSystem = ConstraintSystemImpl()
        constraintSystem.registerTypeVariables(freeParameters)
        constraintSystem.registerTypeVariables(otherType.freeParameters)

        when (matchKind) {
            MatchKind.IS_SUBTYPE -> constraintSystem.addSubtypeConstraint(type, otherType.type, ConstraintPositionKind.RECEIVER_POSITION.position())
            MatchKind.IS_SUPERTYPE -> constraintSystem.addSubtypeConstraint(otherType.type, type, ConstraintPositionKind.RECEIVER_POSITION.position())
        }

        constraintSystem.fixVariables()

        if (constraintSystem.getStatus().hasContradiction()) return null

        // currently ConstraintSystem return successful status in case there are problems with nullability
        // that's why we have to check subtyping manually
        val substitutor = constraintSystem.getResultingSubstitutor()
        val substitutedType = substitutor.substitute(type, Variance.INVARIANT) ?: return null
        if (substitutedType.isError) return null
        val otherSubstitutedType = substitutor.substitute(otherType.type, Variance.INVARIANT) ?: return null
        if (otherSubstitutedType.isError) return null
        return if (substitutedType.checkInheritance(otherSubstitutedType)) constraintSystem.getPartialSubstitutor() else null
    }
}
