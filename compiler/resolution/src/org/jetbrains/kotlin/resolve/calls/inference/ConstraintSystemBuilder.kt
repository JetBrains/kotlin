/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType

interface ConstraintSystemOperation {
    val hasContradiction: Boolean
    fun registerVariable(variable: NewTypeVariable)

    fun addSubtypeConstraint(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition)
    fun addEqualityConstraint(a: UnwrappedType, b: UnwrappedType, position: ConstraintPosition)

    fun isProperType(type: UnwrappedType): Boolean

    fun getProperSuperTypeConstructors(type: UnwrappedType): List<TypeConstructor>
}

interface ConstraintSystemBuilder : ConstraintSystemOperation {
    fun addInnerCall(innerCall: ResolvedKotlinCall.OnlyResolvedKotlinCall)
    fun addPostponedArgument(postponedArgument: PostponedKotlinCallArgument)

    // if runOperations return true, then this operation will be applied, and function return true
    fun runTransaction(runOperations: ConstraintSystemOperation.() -> Boolean): Boolean

    fun buildCurrentSubstitutor(): NewTypeSubstitutor

    /**
     * This function removes variables for which we know exact type.
     * @return substitutor from typeVariable to result
     */
    fun simplify(): NewTypeSubstitutor
}

fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition) =
        runTransaction {
            if (!hasContradiction) addSubtypeConstraint(lowerType, upperType, position)
            !hasContradiction
        }