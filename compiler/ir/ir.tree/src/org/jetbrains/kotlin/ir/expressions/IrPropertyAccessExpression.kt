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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrPropertyAccessExpression : IrMemberAccessExpression {
    override val descriptor: PropertyDescriptor
    val operator: IrOperator?
}

interface IrGetPropertyExpression : IrPropertyAccessExpression

interface IrSetPropertyExpression : IrPropertyAccessExpression {
    var value: IrExpression
}

class IrGetPropertyExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        isSafe: Boolean,
        override val descriptor: PropertyDescriptor,
        override val operator: IrOperator? = null
) : IrMemberAccessExpressionBase(startOffset, endOffset, type, isSafe), IrGetPropertyExpression {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitGetProperty(this, data)
}

class IrSetPropertyExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        isSafe: Boolean,
        override val descriptor: PropertyDescriptor,
        override val operator: IrOperator? = null
) : IrMemberAccessExpressionBase(startOffset, endOffset, null, isSafe), IrSetPropertyExpression {
    constructor(
            startOffset: Int,
            endOffset: Int,
            isSafe: Boolean,
            descriptor: PropertyDescriptor,
            value: IrExpression,
            operator: IrOperator? = null
    ) : this(startOffset, endOffset, isSafe, descriptor, operator) {
        this.value = value
    }

    private var valueImpl: IrExpression? = null
    override var value: IrExpression
        get() = valueImpl!!
        set(value) {
            value.assertDetached()
            valueImpl?.detach()
            valueImpl = value
            value.setTreeLocation(this, ARGUMENT0_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                ARGUMENT0_SLOT -> value
                else -> super.getChild(slot)
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            ARGUMENT0_SLOT -> value = newChild.assertCast()
            else -> super.replaceChild(slot, newChild)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSetProperty(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        super.acceptChildren(visitor, data)
        value.accept(visitor, data)
    }
}
