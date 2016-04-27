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

import org.jetbrains.kotlin.idea.intentions.loopToCallChain.*
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

object IntroduceIndexMatcher : TransformationMatcher {
    override val indexVariableAllowed: Boolean
        get() = false // old index variable is still needed - cannot introduce another one

    override fun match(state: MatchingState): TransformationMatch.Sequence? {
        if (state.statements.size < 2) return null
        val operand = state.statements.last().isPlusPlusOf() ?: return null
        val restStatements = state.statements.dropLast(1)

        fun isContinueOfThisLoopOrOuter(continueExpression: KtContinueExpression): Boolean {
            val targetLoop = continueExpression.targetLoop() ?: return true
            return targetLoop.isAncestor(state.innerLoop, strict = false)
        }

        // there should be no continuation of the loop in statements before index increment
        if (restStatements.any { statement -> statement.anyDescendantOfType<KtContinueExpression>(::isContinueOfThisLoopOrOuter) }) return null

        val variableInitialization = operand.isVariableInitializedBeforeLoop(state.outerLoop, checkNoOtherUsagesInLoop = false)
                                     ?: return null
        if ((variableInitialization.initializer as? KtConstantExpression)?.text != "0") return null

        val variable = variableInitialization.variable

        if (variable.countWriteUsages(state.outerLoop) > 1) return null // changed somewhere else

        // variable should have no usages except in the initialization + currently matching part of the loop
        //TODO: preform more precise analysis when variable can be used earlier or used later but value overwritten before that
        if (variable.countUsages() != variable.countUsages(state.statements + variableInitialization.initializationStatement)) return null

        val newState = state.copy(statements = restStatements,
                                  indexVariable = variable,
                                  initializationStatementsToDelete = state.initializationStatementsToDelete + variableInitialization.initializationStatement)
        return TransformationMatch.Sequence(emptyList(), newState)
    }
}