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

import org.jetbrains.kotlin.ir.DISPATCH_RECEIVER_INDEX
import org.jetbrains.kotlin.ir.EXTENSION_RECEIVER_INDEX
import org.jetbrains.kotlin.types.KotlinType

interface IrMemberAccessExpression : IrDeclarationReference, IrExpressionOwner {
    var dispatchReceiver: IrExpression?
    var extensionReceiver: IrExpression?
    val isSafe: Boolean
}

abstract class IrMemberAccessExpressionBase(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val isSafe: Boolean
) : IrExpressionBase(startOffset, endOffset, type), IrMemberAccessExpression {
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
}
