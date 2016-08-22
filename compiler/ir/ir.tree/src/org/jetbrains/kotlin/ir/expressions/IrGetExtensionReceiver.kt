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

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrGetExtensionReceiver : IrDeclarationReference, IrExpressionWithCopy {
    override val descriptor: ReceiverParameterDescriptor

    override fun copy(): IrGetExtensionReceiver
}

class IrGetExtensionReceiverImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        descriptor: ReceiverParameterDescriptor
) : IrTerminalDeclarationReferenceBase<ReceiverParameterDescriptor>(startOffset, endOffset, type, descriptor), IrGetExtensionReceiver {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitGetExtensionReceiver(this, data)

    override fun copy(): IrGetExtensionReceiver =
            IrGetExtensionReceiverImpl(startOffset, endOffset, type, descriptor)
}
