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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.result

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FilterTransformationBase
import org.jetbrains.kotlin.psi.*

class CountTransformation(
        loop: KtForExpression,
        private val inputVariable: KtCallableDeclaration,
        initialization: VariableInitialization,
        private val filter: KtExpression?
) : AssignToVariableResultTransformation(loop, initialization) {

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation, reformat: Boolean): ResultTransformation? {
        if (previousTransformation !is FilterTransformationBase) return null
        if (previousTransformation.indexVariable != null) return null
        val newFilter = if (filter == null)
            previousTransformation.effectiveCondition.asExpression(reformat)
        else
            KtPsiFactory(filter).createExpressionByPattern("$0 && $1", previousTransformation.effectiveCondition.asExpression(reformat), filter,
                                                           reformat = reformat)
        return CountTransformation(loop, previousTransformation.inputVariable, initialization, newFilter)
    }

    override val presentation: String
        get() = "count" + (if (filter != null) "{}" else "()")

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val reformat = chainedCallGenerator.reformat
        val call = if (filter != null) {
            val lambda = generateLambda(inputVariable, filter, reformat)
            chainedCallGenerator.generate("count $0:'{}'", lambda)
        }
        else {
            chainedCallGenerator.generate("count()")
        }

        return if (initialization.initializer.isZeroConstant()) {
            call
        }
        else {
            KtPsiFactory(call).createExpressionByPattern("$0 + $1", initialization.initializer, call, reformat = reformat)
        }
    }

    /**
     * Matches:
     *     val variable = 0
     *     for (...) {
     *         ...
     *         variable++ (or ++variable)
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = false

        override val shouldUseInputVariables: Boolean
            get() = false

        override fun match(state: MatchingState): TransformationMatch.Result? {
            val operand = state.statements.singleOrNull()?.isPlusPlusOf() ?: return null
            val initialization = operand.findVariableInitializationBeforeLoop(state.outerLoop, checkNoOtherUsagesInLoop = true) ?: return null

            if (initialization.variable.countUsages(state.outerLoop) != 1) return null // this should be the only usage of this variable inside the loop

            val variableType = initialization.variable.resolveToDescriptorIfAny()?.type ?: return null
            if (!KotlinBuiltIns.isInt(variableType)) return null

            val transformation = CountTransformation(state.outerLoop, state.inputVariable, initialization, null)
            return TransformationMatch.Result(transformation)
        }
    }
}