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

interface IrIfThenElse : IrExpression {
    val operator: IrOperator?
    var condition: IrExpression
    var thenBranch: IrExpression
    var elseBranch: IrExpression?
}

class IrIfThenElseImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?,
        override val operator: IrOperator? = null
) : IrExpressionBase(startOffset, endOffset, type), IrIfThenElse {
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

    private var conditionImpl: IrExpression? = null
    override var condition: IrExpression
        get() = conditionImpl!!
        set(value) {
            value.assertDetached()
            value.detach()
            conditionImpl = value
            value.setTreeLocation(this, IF_CONDITION_SLOT)
        }

    private var thenBranchImpl: IrExpression? = null
    override var thenBranch: IrExpression
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
            visitor.visitIf(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        condition.accept(visitor, data)
        thenBranch.accept(visitor, data)
        elseBranch?.accept(visitor, data)
    }

    companion object {
        // a || b == if (a) true else b
        fun oror(startOffset: Int, endOffset: Int, a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.OROR): IrIfThenElse =
                IrIfThenElseImpl(startOffset, endOffset, b.type!!,
                                 a, IrConstImpl.constTrue(b.startOffset, b.endOffset, b.type!!), b,
                                 operator)

        fun oror(a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.OROR): IrIfThenElse =
                oror(b.startOffset, b.endOffset, a, b, operator)

        fun whenComma(a: IrExpression, b: IrExpression): IrIfThenElse =
                oror(a, b, IrOperator.WHEN_COMMA)

        // a && b == if (a) b else false
        fun andand(startOffset: Int, endOffset: Int, a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.ANDAND): IrIfThenElse =
                IrIfThenElseImpl(startOffset, endOffset, b.type!!,
                                 a, b, IrConstImpl.constFalse(b.startOffset, b.endOffset, b.type!!),
                                 operator)

        fun andand(a: IrExpression, b: IrExpression, operator: IrOperator = IrOperator.ANDAND): IrIfThenElse =
                andand(b.startOffset, b.endOffset, a, b, operator)

    }
}
