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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns

interface IrBackingFieldExpression : IrDeclarationReference {
    override val descriptor: PropertyDescriptor
    val superQualifier: ClassDescriptor?
    var receiver: IrExpression?
    val operator: IrOperator?
}

interface IrGetBackingField : IrBackingFieldExpression

interface IrSetBackingField : IrBackingFieldExpression {
    var value: IrExpression
}

abstract class IrBackingFieldExpressionBase(
        startOffset: Int,
        endOffset: Int,
        descriptor: PropertyDescriptor,
        type: KotlinType,
        override val operator: IrOperator? = null,
        override val superQualifier: ClassDescriptor? = null
) : IrDeclarationReferenceBase<PropertyDescriptor>(startOffset, endOffset, type, descriptor), IrBackingFieldExpression {
    override final var receiver: IrExpression? = null
        set(value) {
            value?.assertDetached()
            field?.detach()
            field = value
            value?.setTreeLocation(this, BACKING_FIELD_RECEIVER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                BACKING_FIELD_RECEIVER_SLOT -> receiver
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            BACKING_FIELD_RECEIVER_SLOT -> receiver = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }
}

class IrGetBackingFieldImpl(
        startOffset: Int,
        endOffset: Int,
        descriptor: PropertyDescriptor,
        operator: IrOperator? = null,
        superQualifier: ClassDescriptor? = null
) : IrBackingFieldExpressionBase(startOffset, endOffset, descriptor, descriptor.type, operator, superQualifier), IrGetBackingField {
    constructor(
            startOffset: Int,
            endOffset: Int,
            descriptor: PropertyDescriptor,
            receiver: IrExpression?,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ) : this(startOffset, endOffset, descriptor, operator, superQualifier) {
        this.receiver = receiver
    }

    override fun getChild(slot: Int): IrElement? =
            super.getChild(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        super.replaceChild(slot, newChild)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitGetBackingField(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        receiver?.accept(visitor, data)
    }
}

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