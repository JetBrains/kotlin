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

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinConstraintSystemCompleter(
        private val resultTypeResolver: ResultTypeResolver,
        private val variableFixationFinder: VariableFixationFinder
) {
    enum class ConstraintSystemCompletionMode {
        FULL,
        PARTIAL
    }

    interface Context : VariableFixationFinder.Context, ResultTypeResolver.Context {
        override val postponedArguments: List<PostponedKotlinCallArgument>
        override val notFixedTypeVariables: Map<TypeConstructor, VariableWithConstraints>

        // type can be proper if it not contains not fixed type variables
        fun canBeProper(type: UnwrappedType): Boolean

        // mutable operations
        fun addError(error: KotlinCallDiagnostic)
        fun fixVariable(variable: NewTypeVariable, resultType: UnwrappedType)
    }

    fun runCompletion(
            c: Context,
            completionMode: ConstraintSystemCompletionMode,
            topLevelType: UnwrappedType,
            analyze: (PostponedKotlinCallArgument) -> Unit
    ) {
        while (true) {
            if (analyzePostponeArgumentIfPossible(c, analyze)) continue

            val variableForFixation = variableFixationFinder.findFirstVariableForFixation(c, completionMode, topLevelType)

            if (shouldWeForceCallableReferenceResolution(completionMode, variableForFixation)) {
                if (forceCallableReferenceResolution(c, analyze)) continue
            }

            if (variableForFixation != null) {
                if (variableForFixation.hasProperConstraint || completionMode == ConstraintSystemCompletionMode.FULL) {
                    val variableWithConstraints = c.notFixedTypeVariables[variableForFixation.variable]!!

                    fixVariable(c, topLevelType, variableWithConstraints)

                    if (!variableForFixation.hasProperConstraint) {
                        c.addError(NotEnoughInformationForTypeParameter(variableWithConstraints.typeVariable))
                    }
                    continue
                }
            }
            break
        }

        if (completionMode == ConstraintSystemCompletionMode.FULL) {
            // force resolution for all not-analyzed argument's
            c.postponedArguments.filterNot { it.analyzed }.forEach(analyze)
        }
    }

    private fun shouldWeForceCallableReferenceResolution(
            completionMode: ConstraintSystemCompletionMode,
            variableForFixation: VariableFixationFinder.VariableForFixation?
    ): Boolean {
        if (completionMode == ConstraintSystemCompletionMode.PARTIAL) return false
        if (variableForFixation != null && variableForFixation.hasProperConstraint) return false

        return true
    }

    // true if we do analyze
    private fun analyzePostponeArgumentIfPossible(c: Context, analyze: (PostponedKotlinCallArgument) -> Unit): Boolean {
        for (argument in getOrderedNotAnalyzedPostponedArguments(c)) {
            if (canWeAnalyzeIt(c, argument)) {
                analyze(argument)
                return true
            }
        }
        return false
    }

    // true if we find some callable reference and run resolution for it. Note that such resolution can be unsuccessful
    private fun forceCallableReferenceResolution(c: Context, analyze: (PostponedKotlinCallArgument) -> Unit): Boolean {
        val callableReferenceArgument = getOrderedNotAnalyzedPostponedArguments(c).
                firstIsInstanceOrNull<PostponedCallableReferenceArgument>() ?: return false

        analyze(callableReferenceArgument)
        return true
    }

    private fun getOrderedNotAnalyzedPostponedArguments(c: Context): List<PostponedKotlinCallArgument> {
        val notAnalyzedArguments = c.postponedArguments.filterNot { it.analyzed }

        // todo insert logic here
        return notAnalyzedArguments
    }


    private fun canWeAnalyzeIt(c: Context, argument: PostponedKotlinCallArgument): Boolean {
        if (argument is PostponedCollectionLiteralArgument || argument.analyzed) return false

        return argument.inputTypes.all { c.canBeProper(it) }
    }

    private fun fixVariable(
            c: Context,
            topLevelType: UnwrappedType,
            variableWithConstraints: VariableWithConstraints
    ) {
        val direction = TypeVariableDirectionCalculator(c, topLevelType).getDirection(variableWithConstraints)

        val resultType = resultTypeResolver.findResultType(c, variableWithConstraints, direction)

        c.fixVariable(variableWithConstraints.typeVariable, resultType)
    }
}