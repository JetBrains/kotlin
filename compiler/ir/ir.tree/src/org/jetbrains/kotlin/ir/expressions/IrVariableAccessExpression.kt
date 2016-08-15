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
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrVariableAccessExpression : IrDeclarationReference {
    override val descriptor: VariableDescriptor
}

interface IrGetVariableExpression : IrVariableAccessExpression

interface IrSetVariableExpression : IrVariableAccessExpression {
    val value: IrExpression
    val operator: IrOperator?
}

class IrGetVariableExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        descriptor: VariableDescriptor
) : IrTerminalDeclarationReferenceBase<VariableDescriptor>(startOffset, endOffset, type, descriptor), IrGetVariableExpression {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitGetVariable(this, data)
}

class IrSetVariableExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        override val descriptor: VariableDescriptor,
        override val operator: IrOperator = IrOperator.EQ
) : IrExpressionBase(startOffset, endOffset, null), IrSetVariableExpression {
    constructor(
            startOffset: Int,
            endOffset: Int,
            descriptor: VariableDescriptor,
            value: IrExpression,
            operator: IrOperator = IrOperator.EQ
    ) : this(startOffset, endOffset, descriptor, operator) {
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
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            ARGUMENT0_SLOT -> value = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSetVariable(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        value.accept(visitor, data)
    }
}
