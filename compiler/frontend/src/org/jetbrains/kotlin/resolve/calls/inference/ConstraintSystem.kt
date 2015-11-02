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
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.derivedFrom
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor

interface ConstraintSystem {
    val status: ConstraintSystemStatus

    /**
     * Returns a set of all non-external type parameter descriptors.
     */
    val typeParameterDescriptors: Set<TypeParameterDescriptor>

    /**
     * Returns a set of all registered type variables.
     */
    val typeVariables: Set<TypeParameterDescriptor>

    fun descriptorToVariable(descriptor: TypeParameterDescriptor): TypeParameterDescriptor

    fun variableToDescriptor(typeVariable: TypeParameterDescriptor): TypeParameterDescriptor

    /**
     * Returns the resulting type constraints of solving the constraint system for specific type parameter descriptor.
     * Throws IllegalArgumentException if the type parameter descriptor is not known to the system.
     */
    fun getTypeBounds(typeVariable: TypeParameterDescriptor): TypeBounds

    /**
     * Returns the result of solving the constraint system (mapping from the type variable to the resulting type projection).
     * In the resulting substitution the following should be of concern:
     * - type constraints
     * - variance of the type variable  // not implemented yet
     * - type parameter bounds (that can bind type variables with each other) // not implemented yet
     * If the addition of the 'expected type' constraint made the system fail,
     * this constraint is not included in the resulting substitution.
     */
    val resultingSubstitutor: TypeSubstitutor

    /**
     * Returns the current result of solving the constraint system (mapping from the type variable to the resulting type projection).
     * If there is no information for type parameter, returns type projection for DONT_CARE type.
     */
    val currentSubstitutor: TypeSubstitutor

    fun getNestedTypeVariables(type: KotlinType): List<TypeParameterDescriptor>

    fun toBuilder(filterConstraintPosition: (ConstraintPosition) -> Boolean = { true }): Builder

    interface Builder {
        /**
         * Registers variables in a constraint system.
         * The type variables for the corresponding function are local, the type variables of inner arguments calls are non-local.
         */
        fun registerTypeVariables(
                typeVariables: Collection<TypeParameterDescriptor>,
                mapToDescriptor: (TypeParameterDescriptor) -> TypeParameterDescriptor = { it },
                external: Boolean = false
        )

        /**
         * Adds a constraint that the constraining type is a subtype of the subject type.
         * Asserts that only subject type may contain registered type variables.
         *
         * For example, for `fun <T> id(t: T) {}` to infer `T` in invocation `id(1)`
         * the constraint "Int is a subtype of T" should be generated where T is a subject type, and Int is a constraining type.
         */
        fun addSubtypeConstraint(constrainingType: KotlinType?, subjectType: KotlinType, constraintPosition: ConstraintPosition)

        /**
         * Adds a constraint that the constraining type is a supertype of the subject type.
         * Asserts that only subject type may contain registered type variables.
         *
         * For example, for `fun <T> create(): T` to infer `T` in invocation `val i: Int = create()`
         * the constraint "Int is a supertype of T" should be generated where T is a subject type, and Int is a constraining type.
         */
        fun addSupertypeConstraint(constrainingType: KotlinType?, subjectType: KotlinType, constraintPosition: ConstraintPosition)

        fun fixVariables()

        fun build(): ConstraintSystem
    }
}

fun ConstraintSystem.filterConstraintsOut(excludePositionKind: ConstraintPositionKind): ConstraintSystem {
    return toBuilder { !it.derivedFrom(excludePositionKind) }.build()
}
