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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.ResolvedKotlinCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedLambdaArgument
import org.jetbrains.kotlin.types.UnwrappedType


interface ConstraintSystemBuilder {
    val hasContradiction: Boolean

    fun registerVariable(variable: NewTypeVariable)

    fun addSubtypeConstraint(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition)
    fun addEqualityConstraint(a: UnwrappedType, b: UnwrappedType, position: ConstraintPosition)

    fun addInnerCall(innerCall: ResolvedKotlinCall.OnlyResolvedKotlinCall)
    fun addLambdaArgument(resolvedLambdaArgument: ResolvedLambdaArgument)

    fun addSubtypeConstraintIfCompatible(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition): Boolean

    fun isProperType(type: UnwrappedType): Boolean

    /**
     * This function removes variables for which we know exact type.
     * @return substitutor from typeVariable to result
     */
    fun simplify(): NewTypeSubstitutor
}
