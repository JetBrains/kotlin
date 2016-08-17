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

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetVariableExpressionImpl
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.expressions.IrSetVariableExpressionImpl
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.toExpectedType

class IrVariableLValueValue(
        val ktElement: KtElement,
        val irOperator: IrOperator?,
        val descriptor: VariableDescriptor
) : IrLValue {
    override fun load(): IrExpression =
            IrGetVariableExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor, irOperator
            )

    override fun store(irExpression: IrExpression): IrExpression =
            IrSetVariableExpressionImpl(
                    ktElement.startOffset, ktElement.endOffset,
                    descriptor, irExpression.toExpectedType(descriptor.type), irOperator
            )
}