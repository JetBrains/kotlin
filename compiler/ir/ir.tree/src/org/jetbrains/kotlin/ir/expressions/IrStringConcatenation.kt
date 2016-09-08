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
import java.util.*

interface IrStringConcatenation : IrExpression {
    val arguments: List<IrExpression>
    fun addArgument(argument: IrExpression)
}

class IrStringConcatenationImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType
) : IrExpressionBase(startOffset, endOffset, type), IrStringConcatenation {
    override val arguments: MutableList<IrExpression> = ArrayList()

    override fun addArgument(argument: IrExpression) {
        argument.assertDetached()
        argument.setTreeLocation(this, arguments.size)
        arguments.add(argument)
    }

    override fun getChild(slot: Int): IrElement? =
            arguments.getOrNull(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        if (slot < 0 || slot >= arguments.size) throwNoSuchSlot(slot)

        newChild.assertDetached()
        arguments[slot].detach()
        arguments[slot] = newChild.assertCast()
        newChild.setTreeLocation(this, slot)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitStringConcatenation(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        arguments.forEach { it.accept(visitor, data) }
    }
}
