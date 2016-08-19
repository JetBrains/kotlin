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

package org.jetbrains.kotlin.psi2ir.generators.values

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.toExpectedType
import org.jetbrains.kotlin.types.KotlinType

class PropertyLValue(
        val ktElement: KtElement,
        val irOperator: IrOperator?,
        val descriptor: PropertyDescriptor,
        val dispatchReceiver: IrExpression?,
        val extensionReceiver: IrExpression?,
        val isSafe: Boolean
) : IrLValue {
    override val type: KotlinType?
        get() = descriptor.type

    private fun IrPropertyAccessExpression.setReceivers() =
            apply {
                dispatchReceiver = this@PropertyLValue.dispatchReceiver
                extensionReceiver = this@PropertyLValue.extensionReceiver
            }

    override fun load(): IrExpression =
            IrGetPropertyExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor.type, isSafe, descriptor, irOperator
            ).setReceivers()

    override fun store(irExpression: IrExpression): IrExpression =
            IrSetPropertyExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    isSafe, descriptor, irExpression.toExpectedType(descriptor.type), irOperator
            ).setReceivers()
}