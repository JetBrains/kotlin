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
import org.jetbrains.kotlin.types.KotlinType

interface IrRematerializableValue : IrValue {
    val irExpression: IrExpression

    override val type: KotlinType?
        get() = irExpression.type
}

fun createRematerializableValue(irExpression: IrExpression): IrRematerializableValue? =
        when (irExpression) {
            is IrConst<*> -> IrRematerializableLiteralValue(irExpression)
            is IrGetVariable -> IrRematerializableVariableValue(irExpression)
            is IrGetExtensionReceiver -> IrRematerializableExtensionReceiverValue(irExpression)
            is IrThisReference -> IrRematerializableThisValue(irExpression)
            else -> null
        }

class IrRematerializableLiteralValue(override val irExpression: IrConst<*>): IrRematerializableValue {
    override fun load(): IrExpression =
            IrConstImpl(irExpression.startOffset, irExpression.endOffset, irExpression.type,
                        irExpression.kind, irExpression.kind.valueOf(irExpression))
}

class IrRematerializableVariableValue(override val irExpression: IrGetVariable) : IrRematerializableValue {
    override fun load(): IrExpression =
            IrGetVariableImpl(irExpression.startOffset, irExpression.endOffset, irExpression.descriptor)
}

class IrRematerializableExtensionReceiverValue(override val irExpression: IrGetExtensionReceiver) : IrRematerializableValue {
    override fun load(): IrExpression =
            IrGetExtensionReceiverImpl(irExpression.startOffset, irExpression.endOffset, irExpression.type, irExpression.descriptor)
}

class IrRematerializableThisValue(override val irExpression: IrThisReference): IrRematerializableValue {
    override fun load(): IrExpression =
            IrThisReferenceImpl(irExpression.startOffset, irExpression.endOffset, irExpression.type, irExpression.classDescriptor)
}
