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
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

class FindAndAssignTransformation(
        private val loop: KtForExpression,
        override val inputVariable: KtCallableDeclaration,
        private val stdlibFunName: String,
        private val initialDeclaration: KtProperty
) : ResultTransformation {

    override val commentSavingRange = PsiChildRange(initialDeclaration, loop.unwrapIfLabeled())
    override val commentRestoringRange = commentSavingRange.withoutLastStatement()

    override val canIncludeFilter: Boolean
        get() = true

    override val canIncludeMap: Boolean
        get() = false

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator, filter: FilterOrMap?, map: FilterOrMap?): KtExpression {
        return if (filter == null) {
            chainedCallGenerator.generate("$stdlibFunName()")
        }
        else {
            val lambda = generateLambda(filter.workingVariable, filter.expression)
            chainedCallGenerator.generate("$stdlibFunName $0:'{}'", lambda)
        }
    }

    override fun convertLoop(resultCallChain: KtExpression): KtExpression {
        initialDeclaration.initializer!!.replace(resultCallChain)
        loop.deleteWithLabels()

        if (!initialDeclaration.hasWriteUsages()) { // change variable to 'val' if possible
            initialDeclaration.valOrVarKeyword.replace(KtPsiFactory(initialDeclaration).createValKeyword())
        }

        return initialDeclaration
    }

    /**
     * Matches:
     *     val variable = ...
     *     for (...) {
     *         ...
     *         variable = ...
     *         break
     *     }
     */
    object Matcher : ResultTransformationMatcher {
        override fun match(state: MatchingState): ResultTransformationMatch? {
            //TODO: pass indexVariable as null if not used
            if (state.indexVariable != null) return null

            if (state.statements.size != 2) return null

            val breakExpression = state.statements.last() as? KtBreakExpression ?: return null
            if (!breakExpression.isBreakOrContinueOfLoop(state.outerLoop)) return null

            val binaryExpression = state.statements.first() as? KtBinaryExpression ?: return null
            if (binaryExpression.operationToken != KtTokens.EQ) return null
            val left = binaryExpression.left ?: return null
            val right = binaryExpression.right ?: return null

            //TODO: support also assignment instead of declaration
            val declarationBeforeLoop = state.outerLoop.previousStatement() as? KtProperty ?: return null
            val initializer = declarationBeforeLoop.initializer ?: return null
            if (!left.isVariableReference(declarationBeforeLoop)) return null

            val usageCountInLoop = ReferencesSearch.search(declarationBeforeLoop, LocalSearchScope(state.outerLoop)).count()
            if (usageCountInLoop != 1) return null // this should be the only usage of this variable inside the loop

            val stdlibFunName = stdlibFunNameForFind(right, initializer, state.workingVariable) ?: return null

            val transformation = FindAndAssignTransformation(state.outerLoop, state.workingVariable, stdlibFunName, declarationBeforeLoop)
            return ResultTransformationMatch(transformation)
        }
    }
}