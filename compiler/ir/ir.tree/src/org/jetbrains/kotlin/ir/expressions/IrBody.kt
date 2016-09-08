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
import java.util.*

interface IrBody : IrElement

interface IrExpressionBody : IrBody {
    var expression: IrExpression
}

interface IrBlockBody : IrBody {
    val statements: List<IrStatement>
}

interface IrSyntheticBody : IrBody {
    val kind: IrSyntheticBodyKind
}

enum class IrSyntheticBodyKind {
    ENUM_VALUES,
    ENUM_VALUEOF
}

class IrExpressionBodyImpl(startOffset: Int, endOffset: Int) : IrElementBase(startOffset, endOffset), IrExpressionBody {
    constructor(startOffset: Int, endOffset: Int, expression: IrExpression) : this(startOffset, endOffset) {
        this.expression = expression
    }

    private var expressionImpl: IrExpression? = null
    override var expression: IrExpression
        get() = expressionImpl!!
        set(newValue) {
            newValue.assertDetached()
            expressionImpl?.detach()
            expressionImpl = newValue
            newValue.setTreeLocation(this, CHILD_EXPRESSION_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                CHILD_EXPRESSION_SLOT -> expression
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            CHILD_EXPRESSION_SLOT -> expression = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitExpressionBody(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }
}

class IrBlockBodyImpl(startOffset: Int, endOffset: Int) : IrElementBase(startOffset, endOffset), IrBlockBody {
    override val statements: MutableList<IrStatement> = ArrayList()

    fun addStatement(statement: IrStatement) {
        statement.assertDetached()
        statement.setTreeLocation(this, statements.size)
        statements.add(statement)
    }

    override fun getChild(slot: Int): IrElement? =
            statements.getOrNull(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        if (slot < 0 || slot >= statements.size) throwNoSuchSlot(slot)

        newChild.assertDetached()
        statements[slot].detach()
        statements[slot] = newChild.assertCast()
        newChild.setTreeLocation(this, slot)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitBlockBody(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }
}

class IrSyntheticBodyImpl(startOffset: Int, endOffset: Int, override val kind: IrSyntheticBodyKind) : IrElementBase(startOffset, endOffset), IrSyntheticBody {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSyntheticBody(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun getChild(slot: Int): IrElement? = null

    override fun replaceChild(slot: Int, newChild: IrElement) {
        throwNoSuchSlot(slot)
    }
}