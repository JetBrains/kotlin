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
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList

interface IrVarargElement : IrElement

interface IrVararg : IrExpression {
    val varargElementType : KotlinType
    val elements: List<IrVarargElement>
}

interface IrSpreadElement : IrVarargElement {
    var expression: IrExpression
}

class IrVarargImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val varargElementType: KotlinType
) : IrVararg, IrExpressionBase(startOffset, endOffset, type) {
    override val elements: MutableList<IrVarargElement> = SmartList()

    fun addElement(varargElement: IrVarargElement) {
        varargElement.assertDetached()
        varargElement.setTreeLocation(this, elements.size)
        elements.add(varargElement)
    }

    override fun getChild(slot: Int): IrElement? =
            elements.getOrNull(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        if (slot < 0 || slot >= elements.size) throwNoSuchSlot(slot)

        newChild.assertDetached()
        elements[slot].detach()
        elements[slot] = newChild.assertCast()
        newChild.setTreeLocation(this, slot)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitVararg(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        elements.forEach { it.accept(visitor, data) }
    }
}

class IrSpreadElementImpl(
        startOffset: Int,
        endOffset: Int
) : IrElementBase(startOffset, endOffset), IrSpreadElement {
    constructor(startOffset: Int, endOffset: Int, expression: IrExpression) : this(startOffset, endOffset) {
        this.expression = expression
    }

    private var expressionImpl: IrExpression? = null
    override var expression: IrExpression
        get() = expressionImpl!!
        set(value) {
            value.assertDetached()
            expressionImpl?.detach()
            expressionImpl = value
            value.setTreeLocation(this, CHILD_EXPRESSION_SLOT)
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

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSpreadElement(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        expression.accept(visitor, data)
    }


}