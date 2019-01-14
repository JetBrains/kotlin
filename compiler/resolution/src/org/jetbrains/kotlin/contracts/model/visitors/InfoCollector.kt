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

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.contracts.model.ConditionalEffect
import org.jetbrains.kotlin.contracts.model.ESEffect
import org.jetbrains.kotlin.contracts.model.ESExpressionVisitor
import org.jetbrains.kotlin.contracts.model.MutableContextInfo
import org.jetbrains.kotlin.contracts.model.structure.*

class InfoCollector(private val observedEffect: ESEffect) : ESExpressionVisitor<MutableContextInfo> {
    private var isInverted: Boolean = false

    fun collectFromSchema(schema: List<ESEffect>): MutableContextInfo =
        schema.mapNotNull { collectFromEffect(it) }.fold(
            MutableContextInfo.EMPTY,
            { resultingInfo, clauseInfo -> resultingInfo.and(clauseInfo) })

    private fun collectFromEffect(effect: ESEffect): MutableContextInfo? {
        if (effect !is ConditionalEffect) {
            return MutableContextInfo.EMPTY.fire(effect)
        }

        // Check for information from conditional effects
        return when (observedEffect.isImplies(effect.simpleEffect)) {
        // observed effect implies clause's effect => clause's effect was fired => clause's condition is true
            true -> effect.condition.accept(this)

        // Observed effect *may* or *doesn't* implies clause's - no useful information
            null, false -> null
        }
    }

    override fun visitIs(isOperator: ESIs): MutableContextInfo = with(isOperator) {
        if (functor.isNegated != isInverted) MutableContextInfo.EMPTY.notSubtype(left, type) else MutableContextInfo.EMPTY.subtype(
            left,
            type
        )
    }

    override fun visitEqual(equal: ESEqual): MutableContextInfo = with(equal) {
        if (functor.isNegated != isInverted) MutableContextInfo.EMPTY.notEqual(left, right) else MutableContextInfo.EMPTY.equal(left, right)
    }

    override fun visitAnd(and: ESAnd): MutableContextInfo {
        val leftInfo = and.left.accept(this)
        val rightInfo = and.right.accept(this)

        return if (isInverted) leftInfo.or(rightInfo) else leftInfo.and(rightInfo)
    }

    override fun visitNot(not: ESNot): MutableContextInfo = inverted { not.arg.accept(this) }

    override fun visitOr(or: ESOr): MutableContextInfo {
        val leftInfo = or.left.accept(this)
        val rightInfo = or.right.accept(this)
        return if (isInverted) leftInfo.and(rightInfo) else leftInfo.or(rightInfo)
    }

    override fun visitVariable(esVariable: ESVariable): MutableContextInfo {
        return if (esVariable.type != DefaultBuiltIns.Instance.booleanType)
            MutableContextInfo.EMPTY
        else
            MutableContextInfo.EMPTY.equal(esVariable, isInverted.not().lift())
    }

    override fun visitConstant(esConstant: ESConstant): MutableContextInfo = MutableContextInfo.EMPTY

    override fun visitReceiver(esReceiver: ESReceiver): MutableContextInfo = MutableContextInfo.EMPTY

    private fun <R> inverted(block: () -> R): R {
        isInverted = isInverted.not()
        val result = block()
        isInverted = isInverted.not()
        return result
    }
}

