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

import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

enum class IrTypeOperator {
    CAST,
    IMPLICIT_CAST,
    SAFE_CAST,
    INSTANCEOF,
    NOT_INSTANCEOF;
}

interface IrTypeOperatorExpression : IrExpression {
    val operator: IrTypeOperator
    var argument: IrExpression
    val typeOperand: KotlinType
}

class IrTypeOperatorExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        override val operator: IrTypeOperator,
        override val typeOperand: KotlinType
) : IrExpressionBase(startOffset, endOffset, type), IrTypeOperatorExpression {
    constructor(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType?,
            operator: IrTypeOperator,
            typeOperand: KotlinType,
            argument: IrExpression
    ) : this(startOffset, endOffset, type, operator, typeOperand) {
        this.argument = argument
    }

    private var argumentImpl: IrExpression? = null
    override var argument: IrExpression
        get() = argumentImpl!!
        set(value) {
            value.assertDetached()
            argumentImpl?.detach()
            argumentImpl = value
            value.setTreeLocation(this, ARGUMENT0_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                ARGUMENT0_SLOT -> argument
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            ARGUMENT0_SLOT -> argument = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitTypeOperatorExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument.accept(visitor, data)
    }


}
