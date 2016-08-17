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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrOperatorExpression : IrExpression {
    val operator: IrOperator
    val relatedDescriptor: CallableDescriptor?
}

interface IrUnaryOperatorExpression : IrOperatorExpression {
    var argument: IrExpression
}

interface IrBinaryOperatorExpression : IrOperatorExpression {
    var argument0: IrExpression
    var argument1: IrExpression
}

class IrUnaryOperatorExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        override val operator: IrOperator,
        override val relatedDescriptor: CallableDescriptor?
) : IrExpressionBase(startOffset, endOffset, type), IrUnaryOperatorExpression {
    constructor(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType?,
            operator: IrOperator,
            relatedDescriptor: CallableDescriptor?,
            argument: IrExpression
    ) : this(startOffset, endOffset, type, operator, relatedDescriptor) {
        this.argument = argument
    }

    private var argumentImpl: IrExpression? = null
    override var argument: IrExpression
        get() = argumentImpl!!
        set(value) {
            value.assertDetached()
            argumentImpl?.detach()
            argumentImpl = value
            value.setTreeLocation(this, ARGUMENT0_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                ARGUMENT0_SLOT -> argument
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            ARGUMENT0_SLOT -> argument = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitUnaryOperator(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument.accept(visitor, data)
    }
}

class IrBinaryOperatorExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        override val operator: IrOperator,
        override val relatedDescriptor: CallableDescriptor?
) : IrExpressionBase(startOffset, endOffset, type), IrBinaryOperatorExpression {
    constructor(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType?,
            operator: IrOperator,
            relatedDescriptor: CallableDescriptor?,
            argument0: IrExpression,
            argument1: IrExpression
    ) : this(startOffset, endOffset, type, operator, relatedDescriptor) {
        this.argument0 = argument0
        this.argument1 = argument1
    }

    private var argument0Impl: IrExpression? = null
    override var argument0: IrExpression
        get() = argument0Impl!!
        set(value) {
            value.assertDetached()
            argument0Impl?.detach()
            argument0Impl = value
            value.setTreeLocation(this, ARGUMENT0_SLOT)
        }

    private var argument1Impl: IrExpression? = null
    override var argument1: IrExpression
        get() = argument1Impl!!
        set(value) {
            value.assertDetached()
            argument1Impl?.detach()
            argument1Impl = value
            value.setTreeLocation(this, ARGUMENT1_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                ARGUMENT0_SLOT -> argument0
                ARGUMENT1_SLOT -> argument1
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            ARGUMENT0_SLOT -> argument0 = newChild.assertCast()
            ARGUMENT1_SLOT -> argument1 = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitBinaryOperator(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument0.accept(visitor, data)
        argument1.accept(visitor, data)
    }
}
