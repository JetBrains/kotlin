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

import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrLoop : IrExpression {
    val operator: IrOperator?
    var body: IrExpression
    var condition: IrExpression
}

interface IrWhileLoop : IrLoop

interface IrDoWhileLoop : IrLoop

abstract class IrLoopBase(
        startOffset: Int,
        endOffset: Int,
        override val operator: IrOperator?
) : IrExpressionBase(startOffset, endOffset, null), IrLoop {
    private var conditionImpl: IrExpression? = null
    override var condition: IrExpression
        get() = conditionImpl!!
        set(value) {
            value.assertDetached()
            conditionImpl?.detach()
            conditionImpl = value
            value.setTreeLocation(this, LOOP_CONDITION_SLOT)
        }

    private var bodyImpl: IrExpression? = null
    override var body: IrExpression
        get() = bodyImpl!!
        set(value) {
            value.assertDetached()
            bodyImpl?.detach()
            bodyImpl = value
            value.setTreeLocation(this, LOOP_BODY_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                LOOP_BODY_SLOT -> body
                LOOP_CONDITION_SLOT -> condition
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            LOOP_BODY_SLOT -> body = newChild.assertCast()
            LOOP_CONDITION_SLOT -> condition = newChild.assertCast()
        }
    }
}

class IrWhileLoopImpl(
        startOffset: Int,
        endOffset: Int,
        operator: IrOperator?
) : IrLoopBase(startOffset, endOffset, operator), IrWhileLoop {
    constructor(
            startOffset: Int,
            endOffset: Int,
            operator: IrOperator?,
            condition: IrExpression,
            body: IrExpression
    ) : this(startOffset, endOffset, operator) {
        this.condition = condition
        this.body = body
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitWhileLoop(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        condition.accept(visitor, data)
        body.accept(visitor, data)
    }
}

class IrDoWhileLoopImpl(
        startOffset: Int,
        endOffset: Int,
        operator: IrOperator?
) : IrLoopBase(startOffset, endOffset, operator), IrDoWhileLoop {
    constructor(
            startOffset: Int,
            endOffset: Int,
            operator: IrOperator?,
            body: IrExpression,
            condition: IrExpression
    ) : this(startOffset, endOffset, operator) {
        this.condition = condition
        this.body = body
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitDoWhileLoop(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        body.accept(visitor, data)
        condition.accept(visitor, data)
    }
}