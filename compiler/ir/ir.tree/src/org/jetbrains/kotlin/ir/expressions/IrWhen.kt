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
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList
import java.util.*

interface IrWhen : IrExpression {
    val operator: IrOperator?
    val branchesCount: Int
    fun getNthCondition(n: Int): IrExpression?
    fun getNthResult(n: Int): IrExpression?
    var elseBranch: IrExpression?
}

class IrWhenImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
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

class IrIfThenElseImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        override val operator: IrOperator? = null
) : IrExpressionBase(startOffset, endOffset, type), IrWhen {
    constructor(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType?,
            condition: IrExpression,
            thenBranch: IrExpression,
            elseBranch: IrExpression? = null,
            operator: IrOperator? = null
    ) : this(startOffset, endOffset, type, operator) {
        this.condition = condition
        this.thenBranch = thenBranch
        this.elseBranch = elseBranch
    }

    override val branchesCount: Int get() = 1

    override fun getNthCondition(n: Int): IrExpression? =
            if (n == 0) condition else null

    override fun getNthResult(n: Int): IrExpression? =
            if (n == 0) thenBranch else null

    private var conditionImpl: IrExpression? = null
    var condition: IrExpression
        get() = conditionImpl!!
        set(value) {
            value.assertDetached()
            value.detach()
            conditionImpl = value
            value.setTreeLocation(this, IF_CONDITION_SLOT)
        }

    private var thenBranchImpl: IrExpression? = null
    var thenBranch: IrExpression
        get() = thenBranchImpl!!
        set(value) {
            value.assertDetached()
            value.detach()
            thenBranchImpl = value
            value.setTreeLocation(this, IF_THEN_SLOT)
        }

    override var elseBranch: IrExpression? = null
        set(value) {
            value?.assertDetached()
            value?.detach()
            field = value
            value?.setTreeLocation(this, IF_ELSE_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                IF_CONDITION_SLOT -> condition
                IF_THEN_SLOT -> thenBranch
                IF_ELSE_SLOT -> elseBranch
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        newChild.assertDetached()
        when (slot) {
            IF_CONDITION_SLOT ->
                condition = newChild.assertCast()
            IF_THEN_SLOT ->
                thenBranch = newChild.assertCast()
            IF_ELSE_SLOT ->
                elseBranch = newChild.assertCast()
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitWhen(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        condition.accept(visitor, data)
        thenBranch.accept(visitor, data)
        elseBranch?.accept(visitor, data)
    }

    companion object {
        // a || b == if (a) true else b
        fun oror(startOffset: Int, endOffset: Int, a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.OROR): IrWhen =
                IrIfThenElseImpl(startOffset, endOffset, b.type!!,
                                 a, IrConstImpl.constTrue(b.startOffset, b.endOffset, b.type!!), b,
                                 operator)

        fun oror(a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.OROR): IrWhen =
                oror(b.startOffset, b.endOffset, a, b, operator)

        fun whenComma(a: IrExpression, b: IrExpression): IrWhen =
                oror(a, b, IrOperator.WHEN_COMMA)

        // a && b == if (a) b else false
        fun andand(startOffset: Int, endOffset: Int, a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.ANDAND): IrWhen =
                IrIfThenElseImpl(startOffset, endOffset, b.type!!,
                                 a, b, IrConstImpl.constFalse(b.startOffset, b.endOffset, b.type!!),
                                 operator)

        fun andand(a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.ANDAND): IrWhen =
                andand(b.startOffset, b.endOffset, a, b, operator)

    }
}
