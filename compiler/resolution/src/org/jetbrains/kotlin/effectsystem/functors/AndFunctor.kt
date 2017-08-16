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

class AndFunctor : AbstractSequentialBinaryFunctor() {
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

        // Even if one of 'Returns(true)' is missing, we still can argue that other condition
        // *must* be true when whole functor returns true
        val conditionWhenTrue = applyWithDefault(whenLeftReturnsTrue, whenRightReturnsTrue, { l, r -> l.and(r) })

        // When whole And-functor returns false, we can only argue that one of arguments was false, and to do so we
        // have to know *both* 'Returns(false)'-conditions
        val conditionWhenFalse = applyIfBothNotNull(whenLeftReturnsFalse, whenRightReturnsFalse, { l, r -> l.or(r) })

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