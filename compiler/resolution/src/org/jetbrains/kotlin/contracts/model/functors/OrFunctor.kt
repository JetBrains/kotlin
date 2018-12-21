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
import org.jetbrains.kotlin.contracts.model.structure.*

class OrFunctor(constants: ESConstants) : AbstractBinaryFunctor(constants) {
    override fun invokeWithConstant(computation: Computation, constant: ESConstant): List<ESEffect> = when {
        constant.isFalse -> computation.effects
        constant.isTrue -> emptyList()

        // This means that expression isn't typechecked properly
        else -> computation.effects
    }

    override fun invokeWithReturningEffects(left: List<ConditionalEffect>, right: List<ConditionalEffect>): List<ConditionalEffect> {
        /* Normally, `left` and `right` contain clauses that end with Returns(false/true), but if
         expression wasn't properly typechecked, we could get some senseless clauses here, e.g.
         with Returns(1) (note that they still *return* as guaranteed by AbstractSequentialBinaryFunctor).
         We will just ignore such clauses in order to make smartcasting robust while typing */

        val leftTrue = left.filter { it.simpleEffect.isReturns { value.isTrue } }
        val leftFalse = left.filter { it.simpleEffect.isReturns { value.isFalse } }
        val rightTrue = right.filter { it.simpleEffect.isReturns { value.isTrue } }
        val rightFalse = right.filter { it.simpleEffect.isReturns { value.isFalse } }

        val whenLeftReturnsTrue = foldConditionsWithOr(leftTrue)
        val whenRightReturnsTrue = foldConditionsWithOr(rightTrue)
        val whenLeftReturnsFalse = foldConditionsWithOr(leftFalse)
        val whenRightReturnsFalse = foldConditionsWithOr(rightFalse)

        // When whole Or-functor returns true, all we know is that one of arguments was true.
        // So, to make a correct clause we have to know *both* 'Returns(true)'-conditions
        val conditionWhenTrue = applyIfBothNotNull(whenLeftReturnsTrue, whenRightReturnsTrue) { l, r -> ESOr(constants, l, r) }

        // Even if one of 'Returns(false)' is missing, we still can argue that other condition
        // *must* be false when whole OR-functor returns false
        val conditionWhenFalse = applyWithDefault(whenLeftReturnsFalse, whenRightReturnsFalse) { l, r -> ESAnd(constants, l, r) }

        val result = mutableListOf<ConditionalEffect>()

        if (conditionWhenTrue != null) {
            result.add(ConditionalEffect(conditionWhenTrue, ESReturns(constants.trueValue)))
        }

        if (conditionWhenFalse != null) {
            result.add(ConditionalEffect(conditionWhenFalse, ESReturns(constants.falseValue)))
        }

        return result
    }
}
