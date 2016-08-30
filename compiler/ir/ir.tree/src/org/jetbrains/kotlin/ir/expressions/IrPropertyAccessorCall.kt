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
import org.jetbrains.kotlin.ir.SETTER_ARGUMENT_INDEX
import org.jetbrains.kotlin.ir.assertDetached
import org.jetbrains.kotlin.ir.detach
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrPropertyAccessorCallBase(
        startOffset: Int,
        endOffset: Int,
        override val descriptor: CallableDescriptor,
        override val operator: IrOperator? = null,
        override val superQualifier: ClassDescriptor? = null
) : IrMemberAccessExpressionBase(startOffset, endOffset, descriptor.returnType!!), IrCall {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitCall(this, data)
    }
}

class IrGetterCallImpl(
        startOffset: Int,
        endOffset: Int,
        descriptor: CallableDescriptor,
        operator: IrOperator? = null,
        superQualifier: ClassDescriptor? = null
) : IrPropertyAccessorCallBase(startOffset, endOffset, descriptor, operator, superQualifier), IrCall {
    constructor(
            startOffset: Int,
            endOffset: Int,
            descriptor: CallableDescriptor,
            dispatchReceiver: IrExpression?,
            extensionReceiver: IrExpression?,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ) : this(startOffset, endOffset, descriptor, operator, superQualifier) {
        this.dispatchReceiver = dispatchReceiver
        this.extensionReceiver = extensionReceiver
    }

    override fun getArgument(index: Int): IrExpression? = null

    override fun putArgument(index: Int, valueArgument: IrExpression?) {
        throw UnsupportedOperationException("Property setter call has no arguments")
    }

    override fun removeArgument(index: Int) {
        throw UnsupportedOperationException("Property getter call has no arguments")
    }
}

class IrSetterCallImpl(
        startOffset: Int,
        endOffset: Int,
        descriptor: CallableDescriptor,
        operator: IrOperator? = null,
        superQualifier: ClassDescriptor? = null
) : IrPropertyAccessorCallBase(startOffset, endOffset, descriptor, operator, superQualifier), IrCall {
    constructor(
            startOffset: Int,
            endOffset: Int,
            descriptor: CallableDescriptor,
            dispatchReceiver: IrExpression?,
            extensionReceiver: IrExpression?,
            argument: IrExpression,
            operator: IrOperator? = null,
            superQualifier: ClassDescriptor? = null
    ) : this(startOffset, endOffset, descriptor, operator, superQualifier) {
        this.dispatchReceiver = dispatchReceiver
        this.extensionReceiver = extensionReceiver
        putArgument(SETTER_ARGUMENT_INDEX, argument)
    }

    private var argumentImpl: IrExpression? = null

    override fun getArgument(index: Int): IrExpression? =
            if (index == SETTER_ARGUMENT_INDEX) argumentImpl!! else null

    override fun putArgument(index: Int, valueArgument: IrExpression?) {
        if (index != SETTER_ARGUMENT_INDEX) return
        argumentImpl?.detach()
        valueArgument?.assertDetached()
        argumentImpl = valueArgument
        valueArgument?.setTreeLocation(this, SETTER_ARGUMENT_INDEX)
    }

    override fun removeArgument(index: Int) {
        if (index != SETTER_ARGUMENT_INDEX) return
        argumentImpl?.detach()
        argumentImpl = null
    }
}
