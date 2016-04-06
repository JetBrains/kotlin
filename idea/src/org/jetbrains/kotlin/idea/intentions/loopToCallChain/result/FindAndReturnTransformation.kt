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
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

class FindAndReturnTransformation(
        private val loop: KtForExpression,
        override val inputVariable: KtCallableDeclaration,
        private val generator: (chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?) -> KtExpression,
        private val endReturn: KtReturnExpression,
        private val filter: KtExpression? = null
) : ResultTransformation {

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): ResultTransformation? {
        if (previousTransformation !is FilterTransformation) return null
        assert(filter == null) { "Should not happen because no 2 consecutive FilterTransformation's possible"}
        return FindAndReturnTransformation(loop, previousTransformation.inputVariable, generator, endReturn, previousTransformation.buildRealCondition())
    }

    override val commentSavingRange = PsiChildRange(loop.unwrapIfLabeled(), endReturn)

    override val commentRestoringRange = commentSavingRange.withoutFirstStatement()

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return generator(chainedCallGenerator, filter)
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
        override fun match(state: MatchingState): ResultTransformationMatch? {
            //TODO: pass indexVariable as null if not used
            if (state.indexVariable != null) return null

            val returnInLoop = state.statements.singleOrNull() as? KtReturnExpression ?: return null
            val returnAfterLoop = state.outerLoop.nextStatement() as? KtReturnExpression ?: return null
            if (returnInLoop.getLabelName() != returnAfterLoop.getLabelName()) return null

            val returnValueInLoop = returnInLoop.returnedExpression ?: return null
            val returnValueAfterLoop = returnAfterLoop.returnedExpression ?: return null

            val generator = buildFindOperationGenerator(returnValueInLoop, returnValueAfterLoop, state.workingVariable, findFirst = true) ?: return null

            val transformation = FindAndReturnTransformation(state.outerLoop, state.workingVariable, generator, returnAfterLoop)
            return ResultTransformationMatch(transformation)
        }
    }
}