/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrExpressionOwner : IrElement {
    fun getChildExpression(index: Int): IrExpression?
    fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression)
    fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D)
}

interface IrExpressionOwner1 : IrExpressionOwner {
    var childExpression: IrExpression?

    companion object {
        const val EXPRESSION_INDEX = 0
    }
}

interface IrExpressionOwner2 : IrExpressionOwner {
    var childExpression1: IrExpression?
    var childExpression2: IrExpression?

    companion object {
        const val EXPRESSION1_INDEX = 1
        const val EXPRESSION2_INDEX = 2
    }
}

interface IrExpressionOwnerN : IrExpressionOwner {
    val childExpressions: List<IrExpression>
    fun addChildExpression(child: IrExpression)
    fun removeChildExpression(child: IrExpression)
}
