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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.expressions.IrSetBackingField
import org.jetbrains.kotlin.ir.expressions.impl.IrBackingFieldExpressionBase
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.typeUtil.builtIns

class IrSetBackingFieldImpl(
        startOffset: Int,
        endOffset: Int,
        descriptor: PropertyDescriptor,
        operator: IrOperator? = null,
        superQualifier: ClassDescriptor? = null
) : IrBackingFieldExpressionBase(startOffset, endOffset, descriptor, descriptor.type.builtIns.unitType, operator, superQualifier), IrSetBackingField {
    constructor(
            startOffset: Int,
            endOffset: Int,
            descriptor: PropertyDescriptor,
            receiver: IrExpression?,
            value: IrExpression,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ) : this(startOffset, endOffset, descriptor, operator, superQualifier) {
        this.receiver = receiver
        this.value = value
    }

    private var valueImpl: IrExpression? = null
    override var value: IrExpression
        get() = valueImpl!!
        set(value) {
            value.assertDetached()
            valueImpl?.detach()
            valueImpl = value
            value.setTreeLocation(this, CHILD_EXPRESSION_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                CHILD_EXPRESSION_SLOT -> value
                else -> super.getChild(slot)
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            CHILD_EXPRESSION_SLOT -> value = newChild.assertCast()
            else -> super.replaceChild(slot, newChild)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSetBackingField(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        receiver?.accept(visitor, data)
        value.accept(visitor, data)
    }
}