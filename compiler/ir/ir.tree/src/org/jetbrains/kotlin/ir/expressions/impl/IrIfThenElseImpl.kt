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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

class IrIfThenElseImpl(
        startOffset: Int, endOffset: Int, type: KotlinType,
        override val origin: IrStatementOrigin? = null
) : IrExpressionBase(startOffset, endOffset, type), IrWhen {
    constructor(
            startOffset: Int, endOffset: Int, type: KotlinType,
            condition: IrExpression,
            thenBranch: IrExpression,
            elseBranch: IrExpression? = null,
            origin: IrStatementOrigin? = null
    ) : this(startOffset, endOffset, type, origin) {
        this.condition = condition
        this.thenBranch = thenBranch
        this.elseBranch = elseBranch
    }

    override val branchesCount: Int get() = 1

    override fun getNthCondition(n: Int): IrExpression? =
            if (n == 0) condition else null

    override fun getNthResult(n: Int): IrExpression? =
            when (n) {
                0 -> {
                    thenBranch
                }
                1 -> {
                    elseBranch
                }
                else -> null
            }


    override fun putNthCondition(n: Int, expression: IrExpression) {
        if (n == 0) condition = expression
        else throw AssertionError("No such branch $n")
    }

    override fun putNthResult(n: Int, expression: IrExpression) {
        if (n == 0) thenBranch = expression
        else throw AssertionError("No such branch $n")
    }

    lateinit var condition: IrExpression

    lateinit var thenBranch: IrExpression

    override var elseBranch: IrExpression? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitWhen(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        condition.accept(visitor, data)
        thenBranch.accept(visitor, data)
        elseBranch?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        condition = condition.transform(transformer, data)
        thenBranch = thenBranch.transform(transformer, data)
        elseBranch = elseBranch?.transform(transformer, data)
    }
}