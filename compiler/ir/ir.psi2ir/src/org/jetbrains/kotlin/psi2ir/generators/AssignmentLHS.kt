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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.expressions.IrSetPropertyExpressionImpl
import org.jetbrains.kotlin.ir.expressions.IrSetVariableExpressionImpl
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.KotlinType

interface AssignmentLHS {
    val type: KotlinType?

    fun generateAssignment(ktOperation: KtBinaryExpression, irOperator: IrOperator, irRight: IrExpression): IrExpression
}

class VariableLHS(
        val descriptor: VariableDescriptor
) : AssignmentLHS {
    override val type: KotlinType? = descriptor.type

    override fun generateAssignment(ktOperation: KtBinaryExpression, irOperator: IrOperator, irRight: IrExpression) =
            IrSetVariableExpressionImpl(ktOperation.startOffset, ktOperation.endOffset, descriptor, irRight, irOperator)
}

class PropertyLHS(
        val descriptor: PropertyDescriptor,
        val dispatchReceiver: IrExpression?,
        val extensionReceiver: IrExpression?,
        val isSafe: Boolean
) : AssignmentLHS {
    override val type: KotlinType? = descriptor.type

    override fun generateAssignment(ktOperation: KtBinaryExpression, irOperator: IrOperator, irRight: IrExpression): IrExpression =
            IrSetPropertyExpressionImpl(ktOperation.startOffset, ktOperation.endOffset, isSafe, descriptor, irOperator).apply {
                dispatchReceiver = this@PropertyLHS.dispatchReceiver
                extensionReceiver = this@PropertyLHS.extensionReceiver
                value = irRight
            }
}


