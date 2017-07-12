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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.components.FixationOrderCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.model.PostponedKotlinCallArgument
import org.jetbrains.kotlin.types.UnwrappedType

interface ConstraintSystemCompleter {
    enum class CompletionType {
        FULL,
        PARTIAL
    }

    interface Context {
        val hasContradiction: Boolean
        val postponedArguments: List<PostponedKotlinCallArgument>

        // type can be proper if it not contains not fixed type variables
        fun canBeProper(type: UnwrappedType): Boolean
        fun asFixationOrderCalculatorContext(): FixationOrderCalculator.Context
        fun asResultTypeResolverContext(): ResultTypeResolver.Context

        // mutable operations
        fun addError(error: KotlinCallDiagnostic)
        fun fixVariable(variable: NewTypeVariable, resultType: UnwrappedType)
    }

    fun runCompletion(
            c: Context,
            type: CompletionType,
            topLevelType: UnwrappedType,
            analyze: (PostponedKotlinCallArgument) -> Unit
    )

}