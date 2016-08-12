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
import org.jetbrains.kotlin.ir.ARGUMENT0_INDEX
import org.jetbrains.kotlin.ir.DISPATCH_RECEIVER_INDEX
import org.jetbrains.kotlin.ir.EXTENSION_RECEIVER_INDEX
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrPropertyAccessExpression : IrMemberAccessExpression {
    override val descriptor: PropertyDescriptor
}

interface IrGetPropertyExpression : IrPropertyAccessExpression

interface IrSetPropertyExpression : IrPropertyAccessExpression, IrCompoundExpression1

class IrGetPropertyExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        isSafe: Boolean,
        override val descriptor: PropertyDescriptor
) : IrMemberAccessExpressionBase(startOffset, endOffset, type, isSafe), IrGetPropertyExpression {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitGetProperty(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildExpressions(visitor, data)
    }

    override fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
    }

    override fun getChildExpression(index: Int): IrExpression? =
            when (index) {
                DISPATCH_RECEIVER_INDEX -> dispatchReceiver
                EXTENSION_RECEIVER_INDEX -> extensionReceiver
                else -> null
            }

    override fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression) {
        validateChild(oldChild)
        when (oldChild.index) {
            DISPATCH_RECEIVER_INDEX -> dispatchReceiver = newChild
            EXTENSION_RECEIVER_INDEX -> extensionReceiver = newChild
        }
    }
}

class IrSetPropertyExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        isSafe: Boolean,
        override val descriptor: PropertyDescriptor
) : IrMemberAccessExpressionBase(startOffset, endOffset, type, isSafe), IrSetPropertyExpression {
    override var argument: IrExpression? = null
        set(value) {
            field?.detach()
            field = value
            value?.setTreeLocation(this, ARGUMENT0_INDEX)
        }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSetProperty(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildExpressions(visitor, data)
    }

    override fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
        argument?.accept(visitor, data)
    }

    override fun getChildExpression(index: Int): IrExpression? =
            when (index) {
                DISPATCH_RECEIVER_INDEX -> dispatchReceiver
                EXTENSION_RECEIVER_INDEX -> extensionReceiver
                ARGUMENT0_INDEX -> argument
                else -> null
            }

    override fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression) {
        validateChild(oldChild)
        when (oldChild.index) {
            DISPATCH_RECEIVER_INDEX -> dispatchReceiver = newChild
            EXTENSION_RECEIVER_INDEX -> extensionReceiver = newChild
            ARGUMENT0_INDEX -> argument = newChild
        }
    }
}
