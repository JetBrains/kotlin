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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.registerTypeVariables
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import java.util.HashSet

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

class FuzzyType(
        val type: JetType,
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

    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: JetType) {
        val typeParameter = type.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            typeParameter.getLowerBounds().forEach { addUsedTypeParameters(it) }
            typeParameter.getUpperBounds().forEach { addUsedTypeParameters(it) }
        }

        for (argument in type.getArguments()) {
            addUsedTypeParameters(argument.getType())
        }
    }

    public fun checkIsSubtypeOf(otherType: JetType): TypeSubstitutor?
            = matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)

    public fun checkIsSuperTypeOf(otherType: JetType): TypeSubstitutor?
            = matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)

    private enum class MatchKind {
        IS_SUBTYPE,
        IS_SUPERTYPE
    }

    private fun matchedSubstitutor(otherType: JetType, matchKind: MatchKind): TypeSubstitutor? {
        if (type.isError()) return null
        if (otherType.isError()) return null

        fun JetType.checkInheritance(otherType: JetType): Boolean {
            return when (matchKind) {
                MatchKind.IS_SUBTYPE -> this.isSubtypeOf(otherType)
                MatchKind.IS_SUPERTYPE -> otherType.isSubtypeOf(this)
            }
        }

        if (freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType)) TypeSubstitutor.EMPTY else null
        }

        val constraintSystem = ConstraintSystemImpl()
        constraintSystem.registerTypeVariables(freeParameters, { Variance.INVARIANT })

        when (matchKind) {
            MatchKind.IS_SUBTYPE -> constraintSystem.addSubtypeConstraint(type, otherType, ConstraintPositionKind.SPECIAL.position())
            MatchKind.IS_SUPERTYPE -> constraintSystem.addSubtypeConstraint(otherType, type, ConstraintPositionKind.SPECIAL.position())
        }

        constraintSystem.fixVariables()

        if (!constraintSystem.getStatus().hasContradiction()) {
            // currently ConstraintSystem return successful status in case there are problems with nullability
            // that's why we have to check subtyping manually
            val substitutor = constraintSystem.getResultingSubstitutor()
            val substitutedType = substitutor.substitute(type, Variance.INVARIANT)
            return if (substitutedType != null && substitutedType.checkInheritance(otherType)) substitutor else null
        }
        else {
            return null
        }
    }
}
