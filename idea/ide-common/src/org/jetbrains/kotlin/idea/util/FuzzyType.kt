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

@file:JvmName("FuzzyTypeUtils")

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.CallHandle
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.StrictEqualityTypeChecker
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

fun CallableDescriptor.fuzzyReturnType() = returnType?.toFuzzyType(typeParameters)
fun CallableDescriptor.fuzzyExtensionReceiverType() = extensionReceiverParameter?.type?.toFuzzyType(typeParameters)

fun FuzzyType.makeNotNullable() = type.makeNotNullable().toFuzzyType(freeParameters)
fun FuzzyType.makeNullable() = type.makeNullable().toFuzzyType(freeParameters)
fun FuzzyType.nullability() = type.nullability()

fun FuzzyType.isAlmostEverything(): Boolean {
    if (freeParameters.isEmpty()) return false
    val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    if (typeParameter !in freeParameters) return false
    return typeParameter.upperBounds.singleOrNull()?.isAnyOrNullableAny() ?: false
}

/**
 * Replaces free parameters inside the type with corresponding type parameters of the class (when possible)
 */
fun FuzzyType.presentationType(): KotlinType {
    if (freeParameters.isEmpty()) return type

    val map = HashMap<TypeConstructor, TypeProjection>()
    for ((argument, typeParameter) in type.arguments.zip(type.constructor.parameters)) {
        if (argument.projectionKind == Variance.INVARIANT) {
            val equalToFreeParameter = freeParameters.firstOrNull {
                StrictEqualityTypeChecker.strictEqualTypes(it.defaultType, argument.type.unwrap())
            } ?: continue

            map[equalToFreeParameter.typeConstructor] = createProjection(typeParameter.defaultType, Variance.INVARIANT, null)
        }
    }
    val substitutor = TypeSubstitutor.create(map)
    return substitutor.substitute(type, Variance.INVARIANT)!!
}

fun KotlinType.toFuzzyType(freeParameters: Collection<TypeParameterDescriptor>) = FuzzyType(this, freeParameters)

class FuzzyType(
        val type: KotlinType,
        freeParameters: Collection<TypeParameterDescriptor>
) {
    val freeParameters: Set<TypeParameterDescriptor>

    init {
        if (freeParameters.isNotEmpty()) {
            // we allow to pass type parameters from another function with the same original in freeParameters
            val usedTypeParameters = HashSet<TypeParameterDescriptor>().apply { addUsedTypeParameters(type) }
            if (usedTypeParameters.isNotEmpty()) {
                val originalFreeParameters = freeParameters.map { it.toOriginal() }.toSet()
                this.freeParameters = usedTypeParameters.filter { it.toOriginal() in originalFreeParameters }.toSet()
            }
            else {
                this.freeParameters = emptySet()
            }
        }
        else {
            this.freeParameters = emptySet()
        }
    }

    // Diagnostic for EA-109046
    @Suppress("USELESS_ELVIS")
    private fun TypeParameterDescriptor.toOriginal(): TypeParameterDescriptor {
        val callableDescriptor = containingDeclaration as? CallableMemberDescriptor ?: return this
        val original = callableDescriptor.original ?: error("original = null for $callableDescriptor")
        val typeParameters = original.typeParameters ?: error("typeParameters = null for $original")
        return typeParameters[index]
    }

    override fun equals(other: Any?) = other is FuzzyType && other.type == type && other.freeParameters == freeParameters

    override fun hashCode() = type.hashCode()

    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: KotlinType) {
        val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            typeParameter.upperBounds.forEach { addUsedTypeParameters(it) }
        }

        for (argument in type.arguments) {
            if (!argument.isStarProjection) { // otherwise we can fall into infinite recursion
                addUsedTypeParameters(argument.type)
            }
        }
    }

    fun checkIsSubtypeOf(otherType: FuzzyType): TypeSubstitutor?
            = matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)

    fun checkIsSuperTypeOf(otherType: FuzzyType): TypeSubstitutor?
            = matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)

    fun checkIsSubtypeOf(otherType: KotlinType): TypeSubstitutor?
            = checkIsSubtypeOf(otherType.toFuzzyType(emptyList()))

    fun checkIsSuperTypeOf(otherType: KotlinType): TypeSubstitutor?
            = checkIsSuperTypeOf(otherType.toFuzzyType(emptyList()))

    private enum class MatchKind {
        IS_SUBTYPE,
        IS_SUPERTYPE
    }

    private fun matchedSubstitutor(otherType: FuzzyType, matchKind: MatchKind): TypeSubstitutor? {
        if (type.isError) return null
        if (otherType.type.isError) return null
        if (otherType.type.isUnit() && matchKind == MatchKind.IS_SUBTYPE) return TypeSubstitutor.EMPTY

        fun KotlinType.checkInheritance(otherType: KotlinType): Boolean {
            return when (matchKind) {
                MatchKind.IS_SUBTYPE -> this.isSubtypeOf(otherType)
                MatchKind.IS_SUPERTYPE -> otherType.isSubtypeOf(this)
            }
        }

        if (freeParameters.isEmpty() && otherType.freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType.type)) TypeSubstitutor.EMPTY else null
        }

        val builder = ConstraintSystemBuilderImpl()
        val typeVariableSubstitutor = builder.registerTypeVariables(CallHandle.NONE, freeParameters + otherType.freeParameters)

        val typeInSystem = typeVariableSubstitutor.substitute(type, Variance.INVARIANT)
        val otherTypeInSystem = typeVariableSubstitutor.substitute(otherType.type, Variance.INVARIANT)

        when (matchKind) {
            MatchKind.IS_SUBTYPE ->
                builder.addSubtypeConstraint(typeInSystem, otherTypeInSystem, ConstraintPositionKind.RECEIVER_POSITION.position())
            MatchKind.IS_SUPERTYPE ->
                builder.addSubtypeConstraint(otherTypeInSystem, typeInSystem, ConstraintPositionKind.RECEIVER_POSITION.position())
        }

        builder.fixVariables()

        val constraintSystem = builder.build()

        if (constraintSystem.status.hasContradiction()) return null

        // currently ConstraintSystem return successful status in case there are problems with nullability
        // that's why we have to check subtyping manually
        val substitutor = constraintSystem.resultingSubstitutor
        val substitutedType = substitutor.substitute(type, Variance.INVARIANT) ?: return null
        if (substitutedType.isError) return TypeSubstitutor.EMPTY
        val otherSubstitutedType = substitutor.substitute(otherType.type, Variance.INVARIANT) ?: return null
        if (otherSubstitutedType.isError) return TypeSubstitutor.EMPTY
        if (!substitutedType.checkInheritance(otherSubstitutedType)) return null

        val substitutorToKeepCapturedTypes = object : DelegatedTypeSubstitution(substitutor.substitution) {
            override fun approximateCapturedTypes() = false
        }.buildSubstitutor()

        val substitutionMap: Map<TypeConstructor, TypeProjection> = constraintSystem.typeVariables
                .map { it.originalTypeParameter }
                .associateBy(
                        keySelector = { it.typeConstructor },
                        valueTransform = {
                            val typeProjection = TypeProjectionImpl(Variance.INVARIANT, it.defaultType)
                            val substitutedProjection = substitutorToKeepCapturedTypes.substitute(typeProjection)
                            substitutedProjection?.takeUnless { ErrorUtils.containsUninferredParameter(it.type) } ?: typeProjection
                        })
        return TypeConstructorSubstitution.createByConstructorsMap(substitutionMap, approximateCapturedTypes = true).buildSubstitutor()
    }
}


fun TypeSubstitution.hasConflictWith(other: TypeSubstitution, freeParameters: Collection<TypeParameterDescriptor>): Boolean {
    return freeParameters.any { parameter ->
        val type = parameter.defaultType
        val substituted1 = this[type] ?: return@any false
        val substituted2 = other[type] ?: return@any false
        !StrictEqualityTypeChecker.strictEqualTypes(substituted1.type.unwrap(), substituted2.type.unwrap()) || substituted1.projectionKind != substituted2.projectionKind
    }
}

fun TypeSubstitutor.combineIfNoConflicts(other: TypeSubstitutor, freeParameters: Collection<TypeParameterDescriptor>): TypeSubstitutor? {
    if (this.substitution.hasConflictWith(other.substitution, freeParameters)) return null
    return TypeSubstitutor.createChainedSubstitutor(this.substitution, other.substitution)
}
