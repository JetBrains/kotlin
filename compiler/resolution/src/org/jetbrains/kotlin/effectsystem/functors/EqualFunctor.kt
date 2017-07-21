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
import org.jetbrains.kotlin.effectsystem.factories.UNKNOWN_CONSTANT
import org.jetbrains.kotlin.effectsystem.factories.createClause
import org.jetbrains.kotlin.effectsystem.factories.lift
import org.jetbrains.kotlin.effectsystem.impls.ESConstant
import org.jetbrains.kotlin.effectsystem.impls.ESEqual
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.effectsystem.structure.ESClause
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class EqualsToBinaryConstantFunctor(val isNegated: Boolean, val constant: ESConstant) : AbstractSequentialUnaryFunctor() {
    override fun combineClauses(list: List<ESClause>): List<ESClause> {
        // Corner-case when left is variable
        if (list.size == 1 && list.single().effect.safeAs<ESReturns>()?.value is ESVariable) {
            val variable = (list.single().effect as ESReturns).value as ESVariable
            return listOf(createClause(ESEqual(variable, constant, isNegated), ESReturns(true.lift())))
        }

        /**
         * Here we implicitly use the fact that constant is binary (i.e. has exactly two values),
         * so all 'notEqual'-clauses (if any) are the only clauses that can produce false
         */
        val (equal, notEqual) = list.partition { it.effect == ESReturns(constant) || (it.effect as ESReturns).value == UNKNOWN_CONSTANT }

        val whenArgReturnsSameConstant = foldConditionsWithOr(equal)
        val whenArgReturnsOtherConstant = foldConditionsWithOr(notEqual)

        val result = mutableListOf<ESClause>()

        if (whenArgReturnsSameConstant != null) {
            val returnValue = isNegated.not().lift() // true when not negated, false otherwise
            result.add(createClause(whenArgReturnsSameConstant, ESReturns(returnValue)))
        }

        if (whenArgReturnsOtherConstant != null) {
            val returnValue = isNegated.lift()       // false when not negated, true otherwise
            result.add(createClause(whenArgReturnsOtherConstant, ESReturns(returnValue)))
        }

        return result
    }

    fun negated(): EqualsToBinaryConstantFunctor = EqualsToBinaryConstantFunctor(isNegated.not(), constant)
}