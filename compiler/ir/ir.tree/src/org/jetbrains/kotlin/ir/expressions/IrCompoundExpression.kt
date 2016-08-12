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

import org.jetbrains.kotlin.ir.ARGUMENT0_INDEX
import org.jetbrains.kotlin.ir.ARGUMENT1_INDEX
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

interface IrCompoundExpression : IrExpression, IrExpressionOwner

interface IrCompoundExpression1 : IrCompoundExpression, IrExpressionOwner1

interface IrCompoundExpression2 : IrCompoundExpression, IrExpressionOwner2

interface IrCompoundExpressionN : IrCompoundExpression, IrExpressionOwnerN

abstract class IrCompoundExpressionNBase(
        startOffset: Int,
        endOffset: Int,
        override val type: KotlinType
) : IrExpressionBase(startOffset, endOffset, type), IrCompoundExpressionN {
    override val childExpressions: MutableList<IrExpression> = ArrayList()

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        childExpressions.forEach { it.accept(visitor, data) }
    }

    override fun addChildExpression(child: IrExpression) {
        child.setTreeLocation(this, childExpressions.size)
        childExpressions.add(child)
    }

    override fun getChildExpression(index: Int): IrExpression? =
            childExpressions.getOrNull(index)

    override fun removeChildExpression(child: IrExpression) {
        validateChild(child)
        childExpressions.removeAt(child.index)
        for (i in child.index ..childExpressions.size - 1) {
            childExpressions[i].setTreeLocation(parent, i)
        }
        child.detach()
    }

    override fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression) {
        validateChild(oldChild)
        childExpressions[oldChild.index] = newChild
        newChild.setTreeLocation(this, oldChild.index)
        oldChild.detach()
    }

    override fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D) {
        childExpressions.forEach { it.accept(visitor, data) }
    }
}

// TODO IrExpressionBodyImpl vs IrCompoundExpression1Impl: extract common base class?
abstract class IrCompoundExpression1Base(
        startOffset: Int,
        endOffset: Int,
        override val type: KotlinType
) : IrExpressionBase(startOffset, endOffset, type), IrCompoundExpression1 {
    override var argument: IrExpression? = null
        set(newExpression) {
            field?.detach()
            field = newExpression
            newExpression?.setTreeLocation(this, ARGUMENT0_INDEX)
        }

    override fun getChildExpression(index: Int): IrExpression? =
            if (index == ARGUMENT0_INDEX) argument else null

    override fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression) {
        validateChild(oldChild)
        argument = newChild
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildExpressions(visitor, data)
    }

    override fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument?.accept(visitor, data)
    }
}

abstract class IrCompoundExpression2Base(
        startOffset: Int,
        endOffset: Int,
        override val type: KotlinType
) : IrExpressionBase(startOffset, endOffset, type), IrCompoundExpression2 {
    override var argument0: IrExpression? = null
        set(newExpression) {
            field?.detach()
            field = newExpression
            newExpression?.setTreeLocation(this, ARGUMENT0_INDEX)
        }

    override var argument1: IrExpression? = null
        set(newExpression) {
            field?.detach()
            field = newExpression
            newExpression?.setTreeLocation(this, ARGUMENT1_INDEX)
        }

    override fun getChildExpression(index: Int): IrExpression? =
            when (index) {
                ARGUMENT0_INDEX -> argument0
                ARGUMENT1_INDEX -> argument1
                else -> null
            }

    override fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression) {
        validateChild(oldChild)
        when (oldChild.index) {
            ARGUMENT0_INDEX -> argument0 = newChild
            ARGUMENT1_INDEX -> argument1 = newChild
        }
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildExpressions(visitor, data)
    }

    override fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument0?.accept(visitor, data)
        argument1?.accept(visitor, data)
    }
}