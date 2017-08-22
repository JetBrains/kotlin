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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.CallableReferenceKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.LHSResult
import org.jetbrains.kotlin.resolve.calls.model.SubKotlinCallArgument
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

interface ConstraintSystemOperation {
    val hasContradiction: Boolean
    fun registerVariable(variable: NewTypeVariable)

    fun addSubtypeConstraint(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition)
    fun addEqualityConstraint(a: UnwrappedType, b: UnwrappedType, position: ConstraintPosition)

    fun isProperType(type: UnwrappedType): Boolean
    fun isTypeVariable(type: UnwrappedType): Boolean

    fun getProperSuperTypeConstructors(type: UnwrappedType): List<TypeConstructor>
}

interface ConstraintSystemBuilder : ConstraintSystemOperation {
    val builtIns: KotlinBuiltIns
    // if runOperations return true, then this operation will be applied, and function return true
    fun runTransaction(runOperations: ConstraintSystemOperation.() -> Boolean): Boolean

    fun buildCurrentSubstitutor(): NewTypeSubstitutor
}

fun ConstraintSystemBuilder.addSubtypeConstraintIfCompatible(lowerType: UnwrappedType, upperType: UnwrappedType, position: ConstraintPosition) =
        runTransaction {
            if (!hasContradiction) addSubtypeConstraint(lowerType, upperType, position)
            !hasContradiction
        }


fun PostponedArgumentsAnalyzer.Context.addSubsystemForArgument(argument: KotlinCallArgument?) {
    when (argument) {
        is SubKotlinCallArgument -> addOtherSystem(argument.callResult.constraintSystem)
        is CallableReferenceKotlinCallArgument -> {
            addSubsystemForArgument(argument.lhsResult.safeAs<LHSResult.Expression>()?.lshCallArgument)
        }
    }
}