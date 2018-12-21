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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.contracts.model.Computation
import org.jetbrains.kotlin.contracts.model.ConditionalEffect
import org.jetbrains.kotlin.contracts.model.ESEffect
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.structure.*

class EqualsFunctor(constants: ESConstants, val isNegated: Boolean) : AbstractReducingFunctor(constants) {
    /*
        Equals is a bit tricky case to produce clauses, because e.g. if we want to emit "Returns(true)"-clause,
        then we have to guarantee that we know *all* cases when 'true' could've been returned, and join
        them with OR properly.
        To understand this, consider following example:

            foo(x) == bar(x)
            Effects of foo(x): "Returns(true) -> x is String"
            Effects of bar(x): "Returns(true) -> x is Int"

        Of course, we can't say that the whole expression has effect "Returns(true) -> x is String && x is Int"
        because it could've returned in 'true' also when 'foo(x) == false' and 'bar(x) == false', and we don't
        know anything about such cases.

        We don't want to code here fair analysis for general cases, because it's too complex. Instead, we just
        check some specific cases, which are useful enough in practice
     */
    override fun doInvocation(arguments: List<Computation>): List<ESEffect> {
        assert(arguments.size == 2) { "Equals functor expected 2 arguments, got ${arguments.size}" }

        // TODO: AnnotationConstructorCaller kills this with implicit receiver. Investigate, how.
        if (arguments.size != 2) return emptyList()
        return invokeWithArguments(arguments[0], arguments[1])
    }

    fun invokeWithArguments(left: Computation, right: Computation): List<ESEffect> {
        // First, check if both arguments are values: then we can produce both 'true' and 'false' clauses
        if (left is ESValue && right is ESValue) {
            return equateValues(left, right)
        }

        // Second, check is at least one of argument is Constant: then we can produce 'true'-clause and maybe even 'false'
        if (left is ESConstant) {
            return equateCallAndConstant(right, left)
        }
        if (right is ESConstant) {
            return equateCallAndConstant(left, right)
        }

        // Otherwise, don't even try to produce something. We can improve this in future, if we would like to
        return emptyList()
    }

    private fun equateCallAndConstant(call: Computation, constant: ESConstant): List<ESEffect> {
        val resultingClauses = mutableListOf<ESEffect>()

        for (effect in call.effects) {
            if (effect !is ConditionalEffect || effect.simpleEffect !is ESReturns || effect.simpleEffect.value.isWildcard) {
                resultingClauses += effect
                continue
            }

            if (effect.simpleEffect.value == constant) {
                val trueClause = ConditionalEffect(effect.condition, ESReturns(constants.booleanValue(isNegated.not())))
                resultingClauses.add(trueClause)
            }

            if (effect.simpleEffect.value != constant && effect.simpleEffect.value is ESConstant && isSafeToProduceFalse(
                    call,
                    effect.simpleEffect.value,
                    constant
                )) {
                val falseClause = ConditionalEffect(effect.condition, ESReturns(constants.booleanValue(isNegated)))
                resultingClauses.add(falseClause)
            }
        }

        return resultingClauses
    }

    // It is safe to produce false if we're comparing types which are isomorphic to Boolean. For such types we can be sure, that
    // if leftConstant != rightConstant, then this is the only way to produce 'false'.
    private fun isSafeToProduceFalse(leftCall: Computation, leftConstant: ESConstant, rightConstant: ESConstant): Boolean = when {
    // Comparison of Boolean
        KotlinBuiltIns.isBoolean(rightConstant.type) && leftCall.type != null && KotlinBuiltIns.isBoolean(leftCall.type!!) -> true

    // Comparison of NULL/NOT_NULL, which is essentially Boolean
        leftConstant.isNullConstant() && rightConstant.isNullConstant() -> true

        else -> false
    }

    private fun equateValues(left: ESValue, right: ESValue): List<ESEffect> {
        return listOf(
            ConditionalEffect(ESEqual(constants, left, right, isNegated), ESReturns(constants.trueValue)),
            ConditionalEffect(ESEqual(constants, left, right, isNegated.not()), ESReturns(constants.falseValue))
        )
    }
}
