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

package org.jetbrains.kotlin.contracts.model.visitors

import org.jetbrains.kotlin.contracts.model.*
import org.jetbrains.kotlin.contracts.model.structure.*
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

/**
 * Reduces given list of effects by evaluating constant expressions,
 * throwing away senseless checks and infeasible clauses, etc.
 */
class Reducer : ESExpressionVisitor<ESExpression?> {
    fun reduceEffects(schema: List<ESEffect>): List<ESEffect> =
            schema.mapNotNull { reduceEffect(it) }

    private fun reduceEffect(effect: ESEffect): ESEffect? {
        when (effect) {
            is SimpleEffect -> return effect
            is ConditionalEffect -> {
                // Reduce condition
                val reducedCondition = effect.condition.accept(this) ?: return null

                // Filter never executed conditions
                if (reducedCondition is ESConstant && reducedCondition == ESConstant.FALSE) return null

                // Add always firing effects
                if (reducedCondition is ESConstant && reducedCondition == ESConstant.TRUE) return effect.simpleEffect

                // Leave everything else as is
                return effect
            }
        }
    }

    override fun visitIs(isOperator: ESIs): ESExpression {
        val reducedArg = isOperator.left.accept(this) as ESValue

        val result = when (reducedArg) {
            is ESConstant -> reducedArg.type.isSubtypeOf(isOperator.functor.type)
            is ESVariable -> if (reducedArg.type?.isSubtypeOf(isOperator.functor.type) == true) true else null
            else -> throw IllegalStateException("Unknown ESValue: $reducedArg")
        }

        // Result is unknown, do not evaluate
        result ?: return ESIs(reducedArg, isOperator.functor)

        return result.xor(isOperator.functor.isNegated).lift()
    }

    override fun visitEqual(equal: ESEqual): ESExpression {
        val reducedLeft = equal.left.accept(this) as ESValue
        val reducedRight = equal.right

        if (reducedLeft is ESConstant) return (reducedLeft == reducedRight).xor(equal.functor.isNegated).lift()

        return ESEqual(reducedLeft, reducedRight, equal.functor.isNegated)
    }

    override fun visitAnd(and: ESAnd): ESExpression? {
        val reducedLeft = and.left.accept(this) ?: return null
        val reducedRight = and.right.accept(this) ?: return null

        return when {
            reducedLeft == false.lift() || reducedRight == false.lift() -> false.lift()
            reducedLeft == true.lift() -> reducedRight
            reducedRight == true.lift() -> reducedLeft
            else -> ESAnd(reducedLeft, reducedRight)
        }
    }

    override fun visitOr(or: ESOr): ESExpression? {
        val reducedLeft = or.left.accept(this) ?: return null
        val reducedRight = or.right.accept(this) ?: return null

        return when {
            reducedLeft == true.lift() || reducedRight == true.lift() -> true.lift()
            reducedLeft == false.lift() -> reducedRight
            reducedRight == false.lift() -> reducedLeft
            else -> ESOr(reducedLeft, reducedRight)
        }
    }

    override fun visitNot(not: ESNot): ESExpression? {
        val reducedArg = not.arg.accept(this) ?: return null

        return when (reducedArg) {
            ESConstant.TRUE -> ESConstant.FALSE
            ESConstant.FALSE -> ESConstant.TRUE
            else -> reducedArg
        }
    }

    override fun visitVariable(esVariable: ESVariable): ESVariable = esVariable

    override fun visitConstant(esConstant: ESConstant): ESConstant = esConstant
}