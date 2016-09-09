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

import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBase
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class IrWhenImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val operator: IrOperator? = null
) : IrExpressionBase(startOffset, endOffset, type), IrWhen {
    private val branchParts = ArrayList<IrExpression>()

    fun addBranch(condition: IrExpression, result: IrExpression) {
        condition.assertDetached()
        result.assertDetached()
        condition.setTreeLocation(this, branchParts.size)
        branchParts.add(condition)
        result.setTreeLocation(this, branchParts.size)
        branchParts.add(result)
    }

    override var elseBranch: IrExpression? = null
        set(value) {
            value?.assertDetached()
            value?.detach()
            field = value
            value?.setTreeLocation(this, IF_ELSE_SLOT)
        }

    override fun getChild(slot: Int): IrElement? {
        return when (slot) {
            IF_ELSE_SLOT -> elseBranch
            else -> branchParts.getOrNull(slot)
        }
    }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            IF_ELSE_SLOT -> elseBranch = newChild.assertCast()
            in branchParts.indices -> {
                newChild.assertDetached()
                branchParts[slot].detach()
                branchParts[slot] = newChild.assertCast()
                newChild.setTreeLocation(this, slot)
            }
            else ->
                throwNoSuchSlot(slot)
        }
    }

    override val branchesCount: Int get() = branchParts.size / 2

    override fun getNthCondition(n: Int): IrExpression? =
            branchParts.getOrNull(n * 2)

    override fun getNthResult(n: Int): IrExpression? =
            branchParts.getOrNull(n * 2 + 1)

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitWhen(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        branchParts.forEach { it.accept(visitor, data) }
        elseBranch?.accept(visitor, data)
    }
}