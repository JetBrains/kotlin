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

import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.sequence.FilterTransformation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class CountTransformation(
        loop: KtForExpression,
        inputVariable: KtCallableDeclaration,
        initialization: VariableInitialization,
        private val filter: KtExpression?
) : AssignToVariableResultTransformation(loop, inputVariable, initialization) {

    override fun mergeWithPrevious(previousTransformation: SequenceTransformation): ResultTransformation? {
        if (previousTransformation !is FilterTransformation) return null
        assert(filter == null) { "Should not happen because no 2 consecutive FilterTransformation's possible"}
        return CountTransformation(loop, previousTransformation.inputVariable, initialization, previousTransformation.effectiveCondition())
    }

    override val presentation: String
        get() = "count" + (if (filter != null) "{}" else "()")

    override val shouldUseInputVariable: Boolean
        get() = false

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        val call = if (filter != null) {
            val lambda = generateLambda(inputVariable, filter)
            chainedCallGenerator.generate("count $0:'{}'", lambda)
        }
        else {
            chainedCallGenerator.generate("count()")
        }

        if ((initialization.initializer as? KtConstantExpression)?.text == "0") {
            return call
        }
        else {
            return KtPsiFactory(call).createExpressionByPattern("$0 + $1", initialization.initializer, call)
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
    object Matcher : ResultTransformationMatcher {
        override fun match(state: MatchingState): ResultTransformationMatch? {
            val statement = state.statements.singleOrNull() as? KtUnaryExpression ?: return null
            if (statement.operationToken != KtTokens.PLUSPLUS) return null

            val operand = statement.baseExpression
            val initialization = operand?.detectInitializationBeforeLoop(state.outerLoop) ?: return null

            val usageCountInLoop = ReferencesSearch.search(initialization.variable, LocalSearchScope(state.outerLoop)).count()
            if (usageCountInLoop != 1) return null // this should be the only usage of this variable inside the loop

            val variableType = (initialization.variable.resolveToDescriptorIfAny() as? VariableDescriptor)?.type ?: return null
            if (!KotlinBuiltIns.isInt(variableType)) return null

            val transformation = CountTransformation(state.outerLoop, state.inputVariable, initialization, null)
            return ResultTransformationMatch(transformation)
        }
    }
}