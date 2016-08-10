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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrCallExpression : IrCompoundExpression {
    val superQualifier: ClassDescriptor?
    val operator: IrCallableOperator?
    val isSafe: Boolean
    val callee: CallableDescriptor
    var dispatchReceiver: IrExpression?
    var extensionReceiver: IrExpression?

    fun getValueArgument(valueParameterDescriptor: ValueParameterDescriptor): IrExpression?
    fun putValueArgument(valueParameterDescriptor: ValueParameterDescriptor, valueArgument: IrExpression?)
    fun removeValueArgument(valueParameterDescriptor: ValueParameterDescriptor)

    fun <D> acceptValueArguments(visitor: IrElementVisitor<Unit, D>, data: D)
}

fun IrCallExpression.getMappedValueArguments(): List<IrExpression?> =
        callee.valueParameters.mapNotNull { getValueArgument(it) }

abstract class IrCallExpressionBase(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override final val callee: CallableDescriptor,
        override val isSafe: Boolean,
        override val operator: IrCallableOperator?,
        override val superQualifier: ClassDescriptor?
) : IrExpressionBase(startOffset, endOffset, type), IrCallExpression {
    private val argumentsByParameterIndex =
            kotlin.arrayOfNulls<IrExpression>(callee.valueParameters.size)

    override var dispatchReceiver: IrExpression? = null
        set(newReceiver) {
            field?.detach()
            field = newReceiver
            newReceiver?.setTreeLocation(this, DISPATCH_RECEIVER_INDEX)
        }

    override var extensionReceiver: IrExpression? = null
        set(newReceiver) {
            field?.detach()
            field = newReceiver
            newReceiver?.setTreeLocation(this, EXTENSION_RECEIVER_INDEX)
        }

    override fun getValueArgument(valueParameterDescriptor: ValueParameterDescriptor): IrExpression? =
            argumentsByParameterIndex[valueParameterDescriptor.index]

    override fun putValueArgument(valueParameterDescriptor: ValueParameterDescriptor, valueArgument: IrExpression?) {
        putValueArgument(valueParameterDescriptor.index, valueArgument)
    }

    private fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        argumentsByParameterIndex[index]?.detach()
        argumentsByParameterIndex[index] = valueArgument
        valueArgument?.setTreeLocation(this, index)
    }

    override fun removeValueArgument(valueParameterDescriptor: ValueParameterDescriptor) {
        argumentsByParameterIndex[valueParameterDescriptor.index]?.detach()
        argumentsByParameterIndex[valueParameterDescriptor.index] = null
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

    companion object {
        const val DISPATCH_RECEIVER_INDEX = -1
        const val EXTENSION_RECEIVER_INDEX = -2
    }
}

class IrCallExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        callee: CallableDescriptor,
        isSafe: Boolean,
        operator: IrCallableOperator? = null,
        superQualifier: ClassDescriptor? = null
) : IrCallExpressionBase(startOffset, endOffset, type, callee, isSafe, operator, superQualifier), IrCallExpression {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitCallExpression(this, data)
}