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

import org.jetbrains.kotlin.resolve.calls.components.ConstraintSystemCompleter.Context
import org.jetbrains.kotlin.resolve.calls.inference.components.FixationOrderCalculator
import org.jetbrains.kotlin.resolve.calls.inference.components.InferenceStepResolver
import org.jetbrains.kotlin.resolve.calls.model.PostponedCallableReferenceArgument
import org.jetbrains.kotlin.resolve.calls.model.PostponedKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.PostponedLambdaArgument
import org.jetbrains.kotlin.types.UnwrappedType

class InitialConstraintSystemCompleterImpl(
        private val fixationOrderCalculator: FixationOrderCalculator,
        private val inferenceStepResolver: InferenceStepResolver
) : ConstraintSystemCompleter {

    override fun runCompletion(
            c: Context,
            type: ConstraintSystemCompleter.CompletionType,
            topLevelType: UnwrappedType,
            analyze: (PostponedKotlinCallArgument) -> Unit
    ) {
        if (type == ConstraintSystemCompleter.CompletionType.PARTIAL) return

        resolveCallableReferenceArguments(c, analyze)

        while (!oneStepToEndOrLambda(c, topLevelType, analyze)) {
            // do nothing -- be happy
        }
    }

    private fun resolveCallableReferenceArguments(c: Context, analyze: (PostponedKotlinCallArgument) -> Unit) {
        c.postponedArguments.filterIsInstance<PostponedCallableReferenceArgument>().forEach(analyze)
    }

    // true if it is the end (happy or not)
    // every step we fix type variable or analyzeLambda
    private fun oneStepToEndOrLambda(
            c: Context,
            topLevelType: UnwrappedType,
            analyze: (PostponedKotlinCallArgument) -> Unit
    ): Boolean {
        val lambda = c.postponedArguments.find { it is PostponedLambdaArgument && canWeAnalyzeIt(c, it) }
        if (lambda != null) {
            analyze(lambda)
            return false
        }

        val completionOrder = fixationOrderCalculator.computeCompletionOrder(c.asFixationOrderCalculatorContext(), topLevelType)
        return inferenceStepResolver.resolveVariables(c, completionOrder)
    }


    private fun canWeAnalyzeIt(c: Context, lambda: PostponedLambdaArgument): Boolean {
        if (lambda.analyzed) return false

        if (c.hasContradiction) return true

        lambda.receiver?.let {
            if (!c.canBeProper(it)) return false
        }
        return lambda.parameters.all { c.canBeProper(it) }
    }
}