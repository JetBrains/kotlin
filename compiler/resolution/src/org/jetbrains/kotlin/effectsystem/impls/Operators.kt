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

package org.jetbrains.kotlin.effectsystem.impls

import org.jetbrains.kotlin.effectsystem.functors.*
import org.jetbrains.kotlin.effectsystem.structure.ESBooleanExpression
import org.jetbrains.kotlin.effectsystem.structure.ESBooleanOperator
import org.jetbrains.kotlin.effectsystem.structure.ESValue
import org.jetbrains.kotlin.effectsystem.structure.ESExpressionVisitor

class ESAnd(val left: ESBooleanExpression, val right: ESBooleanExpression, override val functor: AndFunctor): ESBooleanOperator {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitAnd(this)
}

class ESOr(val left: ESBooleanExpression, val right: ESBooleanExpression, override val functor: OrFunctor): ESBooleanOperator {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitOr(this)
}

class ESNot(val arg: ESBooleanExpression, override val functor: NotFunctor): ESBooleanOperator {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitNot(this)

}

class ESIs(val left: ESValue, override val functor: IsFunctor): ESBooleanOperator {
    val type = functor.type
    val isNegated = functor.isNegated

    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitIs(this)

}

class ESEqual(val left: ESValue, val right: ESConstant, override val functor: EqualsToBinaryConstantFunctor): ESBooleanOperator {
    constructor(left: ESValue, right: ESConstant, isNegated: Boolean) : this(left, right, EqualsToBinaryConstantFunctor(isNegated, right))

    val isNegated = functor.isNegated

    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitEqual(this)
}
