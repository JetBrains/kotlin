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

import org.jetbrains.kotlin.ir.CHILD_DECLARATION_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrLocalDeclarationExpression<out D : IrMemberDeclaration> : IrExpression, IrDeclarationOwner1 {
    override val childDeclaration: D
}

interface IrLocalVariableDeclarationExpression : IrLocalDeclarationExpression<IrLocalVariable> {
    override var childDeclaration: IrLocalVariable
}

abstract class IrLocalDeclarationExpressionBase(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType?
) : IrExpressionBase(startOffset, endOffset, type), IrLocalVariableDeclarationExpression {
    private var childDeclarationImpl: IrLocalVariable? = null
    override var childDeclaration: IrLocalVariable
        get() = childDeclarationImpl!!
        set(value) {
            childDeclarationImpl?.detach()
            childDeclarationImpl = value
            value.setTreeLocation(this, CHILD_DECLARATION_INDEX)
        }

    override fun getChildDeclaration(index: Int): IrMemberDeclaration? =
            if (index == CHILD_DECLARATION_INDEX) childDeclaration else null

    override fun replaceChildDeclaration(oldChild: IrMemberDeclaration, newChild: IrMemberDeclaration) {
        if (newChild !is IrLocalVariable) throw AssertionError("IrLocalVariable expected: $newChild")
        validateChild(oldChild)
        childDeclaration = newChild
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        childDeclarationImpl?.accept(visitor, data)
    }
}

class IrLocalVariableDeclarationExpressionImpl(
        startOffset: Int,
        endOffset: Int
) : IrLocalDeclarationExpressionBase(startOffset, endOffset, null), IrLocalVariableDeclarationExpression {
    constructor(startOffset: Int, endOffset: Int, childDeclaration: IrLocalVariable) : this(startOffset, endOffset) {
        this.childDeclaration = childDeclaration
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitLocalVariableDeclarationExpression(this, data)
}