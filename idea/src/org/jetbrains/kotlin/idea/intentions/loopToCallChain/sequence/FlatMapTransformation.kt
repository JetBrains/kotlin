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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class FlatMapTransformation(
        override val loop: KtForExpression,
        val inputVariable: KtCallableDeclaration,
        val transform: KtExpression
) : SequenceTransformation {

    override val affectsIndex: Boolean
        get() = true

    override val presentation: String
        get() = "flatMap{}"

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val lambda = generateLambda(inputVariable, transform)
        return chainedCallGenerator.generate("flatMap$0:'{}'", lambda)
    }

    /**
     * Matches:
     *     for (...) {
     *         ...
     *         for (...) {
     *             ...
     *         }
     *     }
     */
    object Matcher : TransformationMatcher {
        override val indexVariableAllowed: Boolean
            get() = true

        override fun match(state: MatchingState): TransformationMatch.Sequence? {
            val nestedLoop = state.statements.singleOrNull() as? KtForExpression ?: return null

            val transform = nestedLoop.loopRange ?: return null
            // check that we iterate over Iterable
            val nestedSequenceType = transform.analyze(BodyResolveMode.PARTIAL).getType(transform) ?: return null
            val builtIns = transform.builtIns
            val iterableType = FuzzyType(builtIns.iterableType, builtIns.iterable.declaredTypeParameters)
            if (iterableType.checkIsSuperTypeOf(nestedSequenceType) == null) return null

            val nestedLoopBody = nestedLoop.body ?: return null
            val newInputVariable = nestedLoop.loopParameter ?: return null

            if (state.indexVariable != null && state.indexVariable.hasUsages(transform)) {
                // if nested loop range uses index, convert to "mapIndexed {...}.flatMap { it }"
                val mapIndexedTransformation = MapTransformation(state.outerLoop, state.inputVariable, state.indexVariable, transform, mapNotNull = false)
                val inputVarExpression = KtPsiFactory(nestedLoop).createExpressionByPattern("$0", state.inputVariable.nameAsSafeName)
                val transformToUse = if (state.lazySequence) inputVarExpression.asSequence() else inputVarExpression
                val flatMapTransformation = FlatMapTransformation(state.outerLoop, state.inputVariable, transformToUse)
                val newState = state.copy(
                        innerLoop = nestedLoop,
                        statements = listOf(nestedLoopBody),
                        inputVariable = newInputVariable
                )
                return TransformationMatch.Sequence(listOf(mapIndexedTransformation, flatMapTransformation), newState)
            }

            val transformToUse = if (state.lazySequence) transform.asSequence() else transform
            val transformation = FlatMapTransformation(state.outerLoop, state.inputVariable, transformToUse)
            val newState = state.copy(
                    innerLoop = nestedLoop,
                    statements = listOf(nestedLoopBody),
                    inputVariable = newInputVariable
            )
            return TransformationMatch.Sequence(transformation, newState)
        }

        private fun KtExpression.asSequence(): KtExpression {
            return KtPsiFactory(this).createExpressionByPattern("$0.asSequence()", this)
        }
    }
}