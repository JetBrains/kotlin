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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.toExpectedType

interface IrValue {
    fun load(): IrExpression
}

inline fun justExpressionValue(crossinline makeExpression: () -> IrExpression) =
        object : IrValue {
            override fun load() = makeExpression()
        }

interface IrReference : IrValue {
    fun store(value: IrExpression): IrExpression
}

class IrVariableReferenceValue(
        val ktElement: KtElement,
        val irOperator: IrOperator?,
        val descriptor: VariableDescriptor
) : IrReference {
    override fun load(): IrExpression =
            IrGetVariableExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor, irOperator
            )

    override fun store(value: IrExpression): IrExpression =
            IrSetVariableExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor, value.toExpectedType(descriptor.type), irOperator
            )
}

class IrPropertyReferenceValue(
        val ktElement: KtElement,
        val irOperator: IrOperator?,
        val descriptor: PropertyDescriptor,
        val dispatchReceiver: IrExpression?,
        val extensionReceiver: IrExpression?,
        val isSafe: Boolean
) : IrReference {
    private fun IrPropertyAccessExpression.setReceivers() =
            apply {
                dispatchReceiver = this@IrPropertyReferenceValue.dispatchReceiver
                extensionReceiver = this@IrPropertyReferenceValue.extensionReceiver
            }

    override fun load(): IrExpression =
            IrGetPropertyExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor.type, isSafe, descriptor, irOperator
            ).setReceivers()

    override fun store(value: IrExpression): IrExpression =
            IrSetPropertyExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    isSafe, descriptor, value.toExpectedType(descriptor.type), irOperator
            ).setReceivers()
}