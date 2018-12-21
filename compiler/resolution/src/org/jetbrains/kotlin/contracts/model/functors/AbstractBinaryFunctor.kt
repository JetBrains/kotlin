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

package org.jetbrains.kotlin.contracts.model.functors

import org.jetbrains.kotlin.contracts.model.Computation
import org.jetbrains.kotlin.contracts.model.ConditionalEffect
import org.jetbrains.kotlin.contracts.model.ESEffect
import org.jetbrains.kotlin.contracts.model.ESExpression
import org.jetbrains.kotlin.contracts.model.structure.*

abstract class AbstractBinaryFunctor(constants: ESConstants) : AbstractReducingFunctor(constants) {
    override fun doInvocation(arguments: List<Computation>): List<ESEffect> {
        assert(arguments.size == 2) { "Wrong size of arguments list for Binary functor: expected 2, got ${arguments.size}" }
        return invokeWithArguments(arguments[0], arguments[1])
    }

    fun invokeWithArguments(left: Computation, right: Computation): List<ESEffect> {
        if (left is ESConstant) return invokeWithConstant(right, left)
        if (right is ESConstant) return invokeWithConstant(left, right)

        val leftValueReturning =
            left.effects.filterIsInstance<ConditionalEffect>().filter { it.simpleEffect.isReturns { !value.isWildcard } }
        val rightValueReturning =
            right.effects.filterIsInstance<ConditionalEffect>().filter { it.simpleEffect.isReturns { !value.isWildcard } }

        val nonInterestingEffects =
            left.effects - leftValueReturning + right.effects - rightValueReturning

        val evaluatedByFunctor = invokeWithReturningEffects(leftValueReturning, rightValueReturning)

        return nonInterestingEffects + evaluatedByFunctor
    }

    protected fun foldConditionsWithOr(list: List<ConditionalEffect>): ESExpression? =
        if (list.isEmpty())
            null
        else
            list.map { it.condition }.reduce { acc, condition -> ESOr(constants, acc, condition) }

    protected abstract fun invokeWithConstant(computation: Computation, constant: ESConstant): List<ESEffect>

    protected abstract fun invokeWithReturningEffects(
        left: List<ConditionalEffect>,
        right: List<ConditionalEffect>
    ): List<ConditionalEffect>
}
