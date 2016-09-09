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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.lang.AssertionError
import java.lang.UnsupportedOperationException

abstract class IrPrimitiveCallBase(
        startOffset: Int,
        endOffset: Int,
        override val operator: IrOperator,
        override val descriptor: CallableDescriptor
) : IrExpressionBase(startOffset, endOffset, descriptor.returnType!!), IrCall {
    override val superQualifier: ClassDescriptor? get() = null
    override var dispatchReceiver: IrExpression?
        get() = null
        set(value) = throw UnsupportedOperationException("Operator call expression can't have a receiver")

    override var extensionReceiver: IrExpression?
        get() = null
        set(value) = throw UnsupportedOperationException("Operator call expression can't have a receiver")

    override fun getArgument(index: Int): IrExpression? = getChild(index)?.assertCast()

    override fun putArgument(index: Int, valueArgument: IrExpression?) {
        replaceChild(index, valueArgument ?: throw AssertionError("Operator call expression can't have a default argument"))
    }

    override fun removeArgument(index: Int) {
        throw AssertionError("Operator call expression can't have a default argument")
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitCall(this, data)
    }
}

class IrNullaryPrimitiveImpl constructor(
        startOffset: Int,
        endOffset: Int,
        operator: IrOperator,
        descriptor: CallableDescriptor
) : IrPrimitiveCallBase(startOffset, endOffset, operator, descriptor) {
    override fun getChild(slot: Int): IrElement? = null

    override fun replaceChild(slot: Int, newChild: IrElement) {
        throwNoSuchSlot(slot)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // no children
    }
}

class IrUnaryPrimitiveImpl private constructor(
        startOffset: Int,
        endOffset: Int,
        operator: IrOperator,
        descriptor: CallableDescriptor
) : IrPrimitiveCallBase(startOffset, endOffset, operator, descriptor) {
    constructor(
            startOffset: Int,
            endOffset: Int,
            operator: IrOperator,
            descriptor: CallableDescriptor,
            argument: IrExpression
    ) : this(startOffset, endOffset, operator, descriptor) {
        this.argument = argument
    }

    private var argumentImpl: IrExpression? = null
    var argument: IrExpression
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

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument.accept(visitor, data)
    }
}

class IrBinaryPrimitiveImpl(
        startOffset: Int,
        endOffset: Int,
        operator: IrOperator,
        descriptor: CallableDescriptor
) : IrPrimitiveCallBase(startOffset, endOffset, operator, descriptor) {
    constructor(
            startOffset: Int,
            endOffset: Int,
            operator: IrOperator,
            descriptor: CallableDescriptor,
            argument0: IrExpression,
            argument1: IrExpression
    ) : this(startOffset, endOffset, operator, descriptor) {
        this.argument0 = argument0
        this.argument1 = argument1
    }

    private var argument0Impl: IrExpression? = null
    var argument0: IrExpression
        get() = argument0Impl!!
        set(value) {
            value.assertDetached()
            argument0Impl?.detach()
            argument0Impl = value
            value.setTreeLocation(this, ARGUMENT0_SLOT)
        }

    private var argument1Impl: IrExpression? = null
    var argument1: IrExpression
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

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument0.accept(visitor, data)
        argument1.accept(visitor, data)
    }
}
