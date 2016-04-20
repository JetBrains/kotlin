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

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FilterTransformation
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

class FindAndReturnTransformation(
        override val loop: KtForExpression,
        private val generator: FindOperatorGenerator,
        private val endReturn: KtReturnExpression,
        private val filter: KtExpression? = null
) : ResultTransformation {

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): ResultTransformation? {
        if (previousTransformation !is FilterTransformation) return null
        assert(filter == null) { "Should not happen because no 2 consecutive FilterTransformation's possible"}
        return FindAndReturnTransformation(loop, generator, endReturn, previousTransformation.effectiveCondition())
    }

    override val commentSavingRange = PsiChildRange(loop.unwrapIfLabeled(), endReturn)

    private val commentRestoringRange = commentSavingRange.withoutFirstStatement()

    override fun commentRestoringRange(convertLoopResult: KtExpression) = commentRestoringRange

    override val presentation: String
        get() = generator.functionName + (if (filter != null) "{}" else "()")

    override val chainCallCount: Int
        get() = generator.chainCallCount

    override val shouldUseInputVariable: Boolean
        get() = generator.shouldUseInputVariable

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return generator.generate(chainedCallGenerator, filter)
    }

    override fun convertLoop(resultCallChain: KtExpression): KtExpression {
        endReturn.returnedExpression!!.replace(resultCallChain)
        loop.deleteWithLabels()
        return endReturn
    }

    /**
     * Matches:
     *     for (...) {
     *         ...
     *         return ...
     *     }
     *     return ...
     */
    object Matcher : ResultTransformationMatcher {
        override val indexVariableUsePossible: Boolean
            get() = false

        override fun match(state: MatchingState): ResultTransformationMatch? {
            val returnInLoop = state.statements.singleOrNull() as? KtReturnExpression ?: return null
            val returnAfterLoop = state.outerLoop.nextStatement() as? KtReturnExpression ?: return null
            if (returnInLoop.getLabelName() != returnAfterLoop.getLabelName()) return null

            val returnValueInLoop = returnInLoop.returnedExpression ?: return null
            val returnValueAfterLoop = returnAfterLoop.returnedExpression ?: return null

            val generator = buildFindOperationGenerator(returnValueInLoop, returnValueAfterLoop, state.inputVariable, findFirst = true) ?: return null

            val transformation = FindAndReturnTransformation(state.outerLoop, generator, returnAfterLoop)
            return ResultTransformationMatch(transformation)
        }
    }
}