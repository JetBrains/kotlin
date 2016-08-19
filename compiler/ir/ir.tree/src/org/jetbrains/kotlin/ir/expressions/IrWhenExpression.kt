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

interface IrWhenExpression : IrExpression {
    var subject: IrVariable?
    val branches: List<IrBranch>
    var elseExpression: IrExpression?

    fun addBranch(branch: IrBranch)
}

interface IrBranch : IrElement {
    val conditions: List<IrExpression>
    var result: IrExpression

    fun addCondition(expression: IrExpression)
}

class IrWhenExpressionImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?
) : IrExpressionBase(startOffset, endOffset, type), IrWhenExpression {
    constructor(
            startOffset: Int,
            endOffset: Int,
            type: KotlinType?,
            subject: IrVariable?
    ) : this(startOffset, endOffset, type) {
        this.subject = subject
    }

    override var subject: IrVariable? = null
        set(value) {
            value?.assertDetached()
            subject?.detach()
            field = value
            field?.setTreeLocation(this, WHEN_SUBJECT_VARIABLE_SLOT)
        }

    override val branches: MutableList<IrBranch> = ArrayList()

    override var elseExpression: IrExpression? = null
        set(value) {
            value?.assertDetached()
            subject?.detach()
            field = value
            field?.setTreeLocation(this, WHEN_ELSE_EXPRESSION_SLOT)
        }

    override fun addBranch(branch: IrBranch) {
        branch.assertDetached()
        branch.setTreeLocation(this, branches.size)
        branches.add(branch)
    }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                WHEN_SUBJECT_VARIABLE_SLOT -> subject
                WHEN_ELSE_EXPRESSION_SLOT -> elseExpression
                else -> branches.getOrNull(slot)
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        newChild.assertDetached()
        when (slot) {
            WHEN_SUBJECT_VARIABLE_SLOT ->
                subject = newChild.assertCast()
            WHEN_ELSE_EXPRESSION_SLOT ->
                elseExpression = newChild.assertCast()
            in branches.indices -> {
                branches[slot].detach()
                branches[slot] = newChild.assertCast()
                newChild.setTreeLocation(this, slot)
            }
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitWhenExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        subject?.accept(visitor, data)
        branches.forEach { it.accept(visitor, data) }
        elseExpression?.accept(visitor, data)
    }

    companion object {
        fun ifThen(startOffset: Int, endOffset: Int, type: KotlinType?, condition: IrExpression, thenExpression: IrExpression) =
                IrWhenExpressionImpl(startOffset, endOffset, type).apply {
                    addBranch(IrBranchImpl(startOffset, endOffset, condition, thenExpression))
                }

        fun ifThenElse(startOffset: Int, endOffset: Int, type: KotlinType?,
                       condition: IrExpression, thenExpression: IrExpression, elseExpression: IrExpression) =
                IrWhenExpressionImpl(startOffset, endOffset, type).apply {
                    addBranch(IrBranchImpl(startOffset, endOffset, condition, thenExpression))
                    this.elseExpression = elseExpression
                }
    }
}

class IrBranchImpl(startOffset: Int, endOffset: Int) : IrElementBase(startOffset, endOffset), IrBranch {
    constructor(startOffset: Int, endOffset: Int, condition: IrExpression, result: IrExpression) : this(startOffset, endOffset) {
        this.result = result
        addCondition(condition)
    }

    override val conditions: MutableList<IrExpression> = SmartList()

    override fun addCondition(expression: IrExpression) {
        expression.assertDetached()
        expression.setTreeLocation(this, conditions.size)
        conditions.add(expression)
    }

    private var resultImpl: IrExpression? = null
    override var result: IrExpression
        get() = resultImpl!!
        set(value) {
            value.assertDetached()
            resultImpl?.detach()
            resultImpl = value
            value.setTreeLocation(this, BRANCH_RESULT_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                BRANCH_RESULT_SLOT -> result
                else -> conditions.getOrNull(slot)
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            BRANCH_RESULT_SLOT ->
                result = newChild.assertCast()
            in conditions.indices -> {
                conditions[slot].detach()
                conditions[slot] = newChild.assertCast()
                newChild.setTreeLocation(this, slot)
            }
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitBranch(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        conditions.forEach { it.accept(visitor, data) }
        resultImpl?.accept(visitor, data)
    }
}