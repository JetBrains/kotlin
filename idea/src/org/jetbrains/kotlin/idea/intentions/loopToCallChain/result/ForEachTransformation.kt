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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.isAssignment
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType

class ForEachTransformation(
        loop: KtForExpression,
        private val inputVariable: KtCallableDeclaration,
        private val indexVariable: KtCallableDeclaration?,
        private val statement: KtExpression
) : ReplaceLoopResultTransformation(loop) {

    private val functionName = if (indexVariable != null) "forEachIndexed" else "forEach"

    override val presentation: String
        get() = functionName + "{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, indexVariable, statement)
        return chainedCallGenerator.generate("$functionName $0:'{}'", lambda)
    }

    /**
     * Matches:
     *     for (...) {
     *         ...
     *         <statement>
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override val shouldUseInputVariables: Boolean
            get() = false

        override fun match(state: MatchingState): TransformationMatch.Result? {
            if (state.previousTransformations.isEmpty()) return null // do not suggest conversion to just ".forEach{}" or ".forEachIndexed{}"

            val statement = state.statements.singleOrNull() ?: return null

            // check if contains assignment to non-qualified variable - in this case only use of lazy sequence is correct
            if (!state.lazySequence
                && statement.anyDescendantOfType<KtBinaryExpression> { isAssignment(it) && it.left is KtNameReferenceExpression }) return null

            //TODO: should we disallow it for complicated statements like loops, if, when?
            val transformation = ForEachTransformation(state.outerLoop, state.inputVariable, state.indexVariable, statement)
            return TransformationMatch.Result(transformation)
        }
    }
}
