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
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance

public trait ConstraintSystem {

    /**
     * Registers variables in a constraint system.
     * The type variables for the corresponding function are local, the type variables of inner arguments calls are non-local.
     */
    public fun registerTypeVariables(
            typeVariables: Collection<TypeParameterDescriptor>,
            variance: (TypeParameterDescriptor) -> Variance,
            mapToOriginal: (TypeParameterDescriptor) -> TypeParameterDescriptor,
            external: Boolean = false
    )

    /**
     * Returns a set of all non-external registered type variables.
     */
    public fun getTypeVariables(): Set<TypeParameterDescriptor>

    /**
     * Adds a constraint that the constraining type is a subtype of the subject type.<p/>
     * Asserts that only subject type may contain registered type variables. <p/>
     *
     * For example, for {@code "fun <T> id(t: T) {}"} to infer <tt>T</tt> in invocation <tt>"id(1)"</tt>
     * should be generated a constraint <tt>"Int is a subtype of T"</tt> where T is a subject type, and Int is a constraining type.
     */
    public fun addSubtypeConstraint(constrainingType: JetType?, subjectType: JetType, constraintPosition: ConstraintPosition)

    /**
     * Adds a constraint that the constraining type is a supertype of the subject type. <p/>
     * Asserts that only subject type may contain registered type variables. <p/>
     *
     * For example, for {@code "fun <T> create() : T"} to infer <tt>T</tt> in invocation <tt>"val i: Int = create()"</tt>
     * should be generated a constraint <tt>"Int is a supertype of T"</tt> where T is a subject type, and Int is a constraining type.
     */
    public fun addSupertypeConstraint(constrainingType: JetType?, subjectType: JetType, constraintPosition: ConstraintPosition)

    public fun getStatus(): ConstraintSystemStatus

    /**
     * Returns the resulting type constraints of solving the constraint system for specific type variable. <p/>
     * Throws IllegalArgumentException if the type variable was not registered.
     */
    public fun getTypeBounds(typeVariable: TypeParameterDescriptor): TypeBounds

    /**
     * Returns a result of solving the constraint system (mapping from the type variable to the resulting type projection). <p/>
     * In the resulting substitution should be concerned: <p/>
     * - type constraints <p/>
     * - variance of the type variable  // not implemented yet <p/>
     * - type parameter bounds (that can bind type variables with each other). // not implemented yet
     * If the addition of the 'expected type' constraint made the system fail,
     * this constraint is not included in the resulting substitution.
     */
    public fun getResultingSubstitutor(): TypeSubstitutor

    /**
     * Returns a current result of solving the constraint system (mapping from the type variable to the resulting type projection).
     * If there is no information for type parameter, returns type projection for DONT_CARE type.
     */
    public fun getCurrentSubstitutor(): TypeSubstitutor
}
