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

package org.jetbrains.kotlin.contracts.model.structure

import org.jetbrains.kotlin.contracts.model.ESExpression
import org.jetbrains.kotlin.contracts.model.ESExpressionVisitor
import org.jetbrains.kotlin.contracts.model.ESOperator
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.functors.*

class ESAnd(val left: ESExpression, val right: ESExpression) : ESOperator {
    override val functor: AndFunctor = AndFunctor()
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitAnd(this)
}

class ESOr(val left: ESExpression, val right: ESExpression) : ESOperator {
    override val functor: OrFunctor = OrFunctor()
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitOr(this)
}

class ESNot(val arg: ESExpression) : ESOperator {
    override val functor = NotFunctor()
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitNot(this)

}

class ESIs(val left: ESValue, override val functor: IsFunctor) : ESOperator {
    val type = functor.type
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitIs(this)
}

class ESEqual(val left: ESValue, val right: ESValue, isNegated: Boolean) : ESOperator {
    override val functor: EqualsFunctor = EqualsFunctor(isNegated)
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitEqual(this)
}

fun ESExpression.and(other: ESExpression?): ESExpression = if (other == null) this else ESAnd(this, other)
fun ESExpression.or(other: ESExpression?): ESExpression = if (other == null) this else ESOr(this, other)