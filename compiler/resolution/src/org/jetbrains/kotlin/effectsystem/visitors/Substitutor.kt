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

package org.jetbrains.kotlin.effectsystem.visitors

import org.jetbrains.kotlin.effectsystem.factories.pureSchema
import org.jetbrains.kotlin.effectsystem.impls.*
import org.jetbrains.kotlin.effectsystem.structure.EffectSchema
import org.jetbrains.kotlin.effectsystem.structure.ESExpressionVisitor

/**
 * Given an [ESExpression], substitutes all variables in it using provided [substitutions] map,
 * and then flattens resulting tree, producing an [EffectSchema], which describes effects
 * of this [ESExpression] with effects of arguments taken into consideration.
 */
class Substitutor(private val substitutions: Map<ESVariable, EffectSchema>) : ESExpressionVisitor<EffectSchema?> {
    override fun visitIs(isOperator: ESIs): EffectSchema? {
        val arg = isOperator.left.accept(this) ?: return null
        return isOperator.functor.apply(arg)
    }

    override fun visitNot(not: ESNot): EffectSchema? {
        val arg = not.arg.accept(this) ?: return null
        return not.functor.apply(arg)
    }

    override fun visitEqual(equal: ESEqual): EffectSchema? {
        val left = equal.left.accept(this) ?: return null
        return equal.functor.apply(left)
    }

    override fun visitAnd(and: ESAnd): EffectSchema? {
        val left = and.left.accept(this) ?: return null
        val right = and.right.accept(this) ?: return null
        return and.functor.apply(left, right)
    }

    override fun visitOr(or: ESOr): EffectSchema? {
        val left = or.left.accept(this) ?: return null
        val right = or.right.accept(this) ?: return null
        return or.functor.apply(left, right)
    }

    override fun visitVariable(esVariable: ESVariable): EffectSchema?
            = substitutions[esVariable] ?: pureSchema(esVariable)

    override fun visitConstant(esConstant: ESConstant): EffectSchema? = pureSchema(esConstant)

    override fun visitLambda(esLambda: ESLambda): EffectSchema? = visitVariable(esLambda)
}