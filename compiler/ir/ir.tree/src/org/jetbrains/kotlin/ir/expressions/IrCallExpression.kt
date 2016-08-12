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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.DISPATCH_RECEIVER_INDEX
import org.jetbrains.kotlin.ir.EXTENSION_RECEIVER_INDEX
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrCallExpression : IrMemberAccessExpression, IrCompoundExpression {
    val superQualifier: ClassDescriptor?
    val operator: IrOperator?
    override val descriptor: CallableDescriptor

    fun getValueArgument(index: Int): IrExpression?
    fun putValueArgument(index: Int, valueArgument: IrExpression?)
    fun removeValueArgument(index: Int)

    fun <D> acceptValueArguments(visitor: IrElementVisitor<Unit, D>, data: D)
}

class IrCallExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val descriptor: CallableDescriptor,
        isSafe: Boolean,
        override val operator: IrOperator?,
        override val superQualifier: ClassDescriptor?
) : IrMemberAccessExpressionBase(startOffset, endOffset, type, isSafe), IrCallExpression {
    private val argumentsByParameterIndex =
            kotlin.arrayOfNulls<IrExpression>(descriptor.valueParameters.size)

    override fun getValueArgument(index: Int): IrExpression? =
            argumentsByParameterIndex[index]

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        argumentsByParameterIndex[index]?.detach()
        argumentsByParameterIndex[index] = valueArgument
        valueArgument?.setTreeLocation(this, index)
    }

    override fun removeValueArgument(index: Int) {
        argumentsByParameterIndex[index]?.detach()
        argumentsByParameterIndex[index] = null
    }

    override fun getChildExpression(index: Int): IrExpression? =
            when (index) {
                DISPATCH_RECEIVER_INDEX ->
                    dispatchReceiver
                EXTENSION_RECEIVER_INDEX ->
                    extensionReceiver
                else ->
                    argumentsByParameterIndex.getOrNull(index)
            }

    override fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression) {
        validateChild(oldChild)
        when (oldChild.index) {
            DISPATCH_RECEIVER_INDEX ->
                dispatchReceiver = newChild
            EXTENSION_RECEIVER_INDEX ->
                extensionReceiver = newChild
            else ->
                putValueArgument(oldChild.index, newChild)
        }
    }

    override fun <D> acceptValueArguments(visitor: IrElementVisitor<Unit, D>, data: D) {
        for (valueArgument in argumentsByParameterIndex) {
            valueArgument?.let { it.accept(visitor, data) }
        }
    }

    override fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
        acceptValueArguments(visitor, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildExpressions(visitor, data)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitCallExpression(this, data)
}
