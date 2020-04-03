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

import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.CallableReferenceKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.LHSResult
import org.jetbrains.kotlin.resolve.calls.model.SubKotlinCallArgument
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

interface ConstraintSystemOperation {
    val hasContradiction: Boolean
    fun registerVariable(variable: TypeVariableMarker)
    fun markPostponedVariable(variable: TypeVariableMarker)
    fun unmarkPostponedVariable(variable: TypeVariableMarker)
    fun removePostponedVariables()

    fun addSubtypeConstraint(lowerType: KotlinTypeMarker, upperType: KotlinTypeMarker, position: ConstraintPosition)
    fun addEqualityConstraint(a: KotlinTypeMarker, b: KotlinTypeMarker, position: ConstraintPosition)

    fun isProperType(type: KotlinTypeMarker): Boolean
    fun isTypeVariable(type: KotlinTypeMarker): Boolean
    fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean

    fun getProperSuperTypeConstructors(type: KotlinTypeMarker): List<TypeConstructorMarker>

    fun addOtherSystem(otherSystem: ConstraintStorage)
}

interface ConstraintSystemBuilder : ConstraintSystemOperation {
    //val builtIns: KotlinBuiltIns
    // if runOperations return true, then this operation will be applied, and function return true
    fun runTransaction(runOperations: ConstraintSystemOperation.() -> Boolean): Boolean

    fun buildCurrentSubstitutor(): TypeSubstitutorMarker

    fun currentStorage(): ConstraintStorage
}

fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(
    lowerType: KotlinTypeMarker,
    upperType: KotlinTypeMarker,
    position: ConstraintPosition
) =
    runTransaction {
        if (!hasContradiction) addSubtypeConstraint(lowerType, upperType, position)
        !hasContradiction
    }


fun PostponedArgumentsAnalyzer.Context.addSubsystemFromArgument(argument: KotlinCallArgument?): Boolean {
    return when (argument) {
        is SubKotlinCallArgument -> {
            addOtherSystem(argument.callResult.constraintSystem)
            true
        }

        is CallableReferenceKotlinCallArgument -> {
            addSubsystemFromArgument(argument.lhsResult.safeAs<LHSResult.Expression>()?.lshCallArgument)
        }

        else -> false
    }
}
