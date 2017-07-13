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
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallDiagnostic
import org.jetbrains.kotlin.resolve.calls.model.PostponedCallableReferenceArgument
import org.jetbrains.kotlin.resolve.calls.model.PostponedKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.PostponedLambdaArgument
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class ConstraintSystemCompleter(
        private val resultTypeResolver: ResultTypeResolver,
        private val variableFixationFinder: VariableFixationFinder
) {
    enum class CompletionType {
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
            type: CompletionType,
            topLevelType: UnwrappedType,
            analyze: (PostponedKotlinCallArgument) -> Unit
    ) {
        while (c.notFixedTypeVariables.isNotEmpty()) {
            if (analyzePostponeArgumentIfPossible(c, analyze)) continue

            val variableForFixation = variableFixationFinder.findFirstVariableForFixation(c, type, topLevelType)
            if (variableForFixation != null) {
                if (variableForFixation.hasProperConstraint || (type == CompletionType.FULL && allCallableReferencesAnalyzed(c))) {
                    val variableWithConstraints = c.notFixedTypeVariables[variableForFixation.variable]!!

                    fixVariable(c, topLevelType, variableWithConstraints)

                    if (!variableForFixation.hasProperConstraint) {
                        c.addError(NotEnoughInformationForTypeParameter(variableWithConstraints.typeVariable))
                    }
                    continue
                }
            }

            if (type == CompletionType.FULL && forceCallableReferenceResolution(c, analyze)) continue

            break
        }

        if (type == CompletionType.FULL) {
            // force resolution for all not-analyzed argument's
            c.postponedArguments.filterNot { it.analyzed }.forEach(analyze)
        }

    }

    // true if we do analyze
    private fun analyzePostponeArgumentIfPossible(c: Context, analyze: (PostponedKotlinCallArgument) -> Unit): Boolean {
        for (argument in getOrderedNotAnalyzedPostponedArguments(c)) {
            val canWeAnalyzeIt = when (argument) {
                is PostponedCallableReferenceArgument -> canWeAnalyzeIt(c, argument)
                is PostponedLambdaArgument -> canWeAnalyzeIt(c, argument)
                else -> false
            }
            if (canWeAnalyzeIt) {
                analyze(argument)
                return true
            }
        }
        return false
    }

    private fun allCallableReferencesAnalyzed(c: Context) =
            c.postponedArguments.all { it.analyzed || it !is PostponedCallableReferenceArgument }

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


    private fun canWeAnalyzeIt(c: Context, lambda: PostponedKotlinCallArgument): Boolean {
        if (lambda.analyzed) return false

        return lambda.inputTypes.all { c.canBeProper(it) }
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