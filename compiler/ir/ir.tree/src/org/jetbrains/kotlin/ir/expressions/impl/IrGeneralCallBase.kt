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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.assertCast
import org.jetbrains.kotlin.ir.assertDetached
import org.jetbrains.kotlin.ir.detach
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGeneralCall
import org.jetbrains.kotlin.ir.expressions.impl.IrMemberAccessExpressionBase
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

abstract class IrGeneralCallBase(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        numArguments: Int,
        override val operator: IrOperator? = null
) : IrMemberAccessExpressionBase(startOffset, endOffset, type), IrGeneralCall {
    protected val argumentsByParameterIndex =
            arrayOfNulls<IrExpression>(numArguments)

    override fun getArgument(index: Int): IrExpression? =
            argumentsByParameterIndex[index]

    override fun putArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= argumentsByParameterIndex.size) {
            throw AssertionError("$this: No such argument slot: $index")
        }
        valueArgument?.assertDetached()
        argumentsByParameterIndex[index]?.detach()
        argumentsByParameterIndex[index] = valueArgument
        valueArgument?.setTreeLocation(this, index)
    }

    override fun removeArgument(index: Int) {
        argumentsByParameterIndex[index]?.detach()
        argumentsByParameterIndex[index] = null
    }

    override fun getChild(slot: Int): IrElement? =
            if (0 <= slot)
                argumentsByParameterIndex.getOrNull(slot)
            else
                super.getChild(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        if (0 <= slot)
            putArgument(slot, newChild.assertCast())
        else
            super.replaceChild(slot, newChild)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        super.acceptChildren(visitor, data)
        argumentsByParameterIndex.forEach { it?.accept(visitor, data) }
    }
}