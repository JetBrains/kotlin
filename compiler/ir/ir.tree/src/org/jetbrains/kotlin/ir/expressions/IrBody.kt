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

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.SourceLocation
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrBody : IrElement {
    override val parent: IrDeclaration
}

interface IrExpressionBody : IrBody, IrExpressionOwner1

abstract class IrBodyBase(sourceLocation: SourceLocation): IrElementBase(sourceLocation), IrBody {
    override lateinit var parent: IrDeclaration
}

// TODO IrExpressionBodyImpl vs IrCompoundExpression1Impl: extract common base class?
class IrExpressionBodyImpl(sourceLocation: SourceLocation) : IrBodyBase(sourceLocation), IrExpressionBody {
    override var childExpression: IrExpression? = null
        set(newExpression) {
            field?.detach()
            field = newExpression
            newExpression?.setTreeLocation(this, IrExpressionOwner1.EXPRESSION_INDEX)
        }

    override fun getChildExpression(index: Int): IrExpression? =
            if (index == IrExpressionOwner1.EXPRESSION_INDEX) childExpression else null

    override fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression) {
        validateChild(oldChild)
        childExpression = newChild
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitExpressionBody(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildExpressions(visitor, data)
    }

    override fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D) {
        childExpression?.accept(visitor, data)
    }
}