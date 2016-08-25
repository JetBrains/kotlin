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
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType


interface IrReturn : IrExpression {
    var value: IrExpression?
    val returnTarget: CallableDescriptor
}

class IrReturnImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val returnTarget: CallableDescriptor
) : IrExpressionBase(startOffset, endOffset, type), IrReturn {
    constructor(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType,
            returnTarget: CallableDescriptor,
            value: IrExpression?
    ) : this(startOffset, endOffset, type, returnTarget) {
        this.value = value
    }

    override var value: IrExpression? = null
        set(newValue) {
            newValue?.assertDetached()
            field?.detach()
            field = newValue
            newValue?.setTreeLocation(this, CHILD_EXPRESSION_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                CHILD_EXPRESSION_SLOT -> value
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            CHILD_EXPRESSION_SLOT -> value = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitReturn(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        value?.accept(visitor, data)
    }


}