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

import org.jetbrains.kotlin.ir.expressions.*

interface IrRematerializableValue : IrValue

fun createRematerializableValue(irExpression: IrExpression): IrRematerializableValue? =
        when (irExpression) {
            is IrLiteralExpression<*> -> IrRematerializableLiteralValue(irExpression)
            is IrGetVariableExpression -> IrRematerializableVariableValue(irExpression)
            is IrGetExtensionReceiverExpression -> IrRematerializableExtensionReceiverValue(irExpression)
            is IrThisExpression -> IrRematerializableThisValue(irExpression)
            else -> null
        }

class IrRematerializableLiteralValue(val irExpression: IrLiteralExpression<*>): IrRematerializableValue {
    override fun load(): IrExpression =
            IrLiteralExpressionImpl(irExpression.startOffset, irExpression.endOffset, irExpression.type,
                                    irExpression.kind, irExpression.kind.valueOf(irExpression))
}

class IrRematerializableVariableValue(val irExpression: IrGetVariableExpression) : IrRematerializableValue {
    override fun load(): IrExpression =
            IrGetVariableExpressionImpl(irExpression.startOffset, irExpression.endOffset, irExpression.descriptor)
}

class IrRematerializableExtensionReceiverValue(val irExpression: IrGetExtensionReceiverExpression) : IrRematerializableValue {
    override fun load(): IrExpression =
            IrGetExtensionReceiverExpressionImpl(irExpression.startOffset, irExpression.endOffset, irExpression.type, irExpression.descriptor)
}

class IrRematerializableThisValue(val irExpression: IrThisExpression): IrRematerializableValue {
    override fun load(): IrExpression =
            IrThisExpressionImpl(irExpression.startOffset, irExpression.endOffset, irExpression.type, irExpression.classDescriptor)
}
