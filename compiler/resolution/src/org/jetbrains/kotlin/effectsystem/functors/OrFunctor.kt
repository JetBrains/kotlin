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

package org.jetbrains.kotlin.effectsystem.functors

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.factories.createClause
import org.jetbrains.kotlin.effectsystem.factories.lift
import org.jetbrains.kotlin.effectsystem.impls.and
import org.jetbrains.kotlin.effectsystem.impls.or
import org.jetbrains.kotlin.effectsystem.structure.ESClause

class OrFunctor : AbstractSequentialBinaryFunctor() {
    override fun combineClauses(left: List<ESClause>, right: List<ESClause>): List<ESClause> {
        /* Normally, `left` and `right` contain clauses that end with Returns(false/true), but if
         expression wasn't properly typechecked, we could get some senseless clauses here, e.g.
         with Returns(1) (note that they still *return* as guaranteed by AbstractSequentialBinaryFunctor).
         We will just ignore such clauses in order to make smartcasting robust while typing */
        val (leftTrue, leftFalse) = left.strictPartition(ESReturns(true.lift()), ESReturns(false.lift()))
        val (rightTrue, rightFalse) = right.strictPartition(ESReturns(true.lift()), ESReturns(false.lift()))

        val whenLeftReturnsTrue = foldConditionsWithOr(leftTrue)
        val whenRightReturnsTrue = foldConditionsWithOr(rightTrue)
        val whenLeftReturnsFalse = foldConditionsWithOr(leftFalse)
        val whenRightReturnsFalse = foldConditionsWithOr(rightFalse)

        // When whole Or-functor returns true, all we know is that one of arguments was true.
        // So, to make a correct clause we have to know *both* 'Returns(true)'-conditions
        val conditionWhenTrue = applyIfBothNotNull(whenLeftReturnsTrue, whenRightReturnsTrue, { l, r -> l.or(r) })

        // Even if one of 'Returns(false)' is missing, we still can argue that other condition
        // *must* be false when whole OR-functor returns false
        val conditionWhenFalse = applyWithDefault(whenLeftReturnsFalse, whenRightReturnsFalse, { l, r -> l.and(r) })

        val result = mutableListOf<ESClause>()

        if (conditionWhenTrue != null) {
            result.add(createClause(conditionWhenTrue, ESReturns(true.lift())))
        }

        if (conditionWhenFalse != null) {
            result.add(createClause(conditionWhenFalse, ESReturns(false.lift())))
        }

        return result
    }
}