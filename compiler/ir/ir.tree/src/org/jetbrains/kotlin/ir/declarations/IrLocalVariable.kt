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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.CHILD_EXPRESSION_INDEX
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrLocalVariable : IrMemberDeclaration, IrExpressionOwner {
    override val descriptor: VariableDescriptor

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.LOCAL_VARIABLE

    var initializerExpression: IrExpression?
}

class IrLocalVariableImpl(
        startOffset: Int,
        endOffset: Int,
        originKind: IrDeclarationOriginKind,
        override val descriptor: VariableDescriptor
) : IrMemberDeclarationBase(startOffset, endOffset, originKind), IrLocalVariable {
    override var initializerExpression: IrExpression? = null
        set(value) {
            field?.detach()
            field = value
            value?.setTreeLocation(this, CHILD_EXPRESSION_INDEX)
        }

    override fun getChildExpression(index: Int): IrExpression? =
            if (index == CHILD_EXPRESSION_INDEX) initializerExpression else null

    override fun replaceChildExpression(oldChild: IrExpression, newChild: IrExpression) {
        validateChild(oldChild)
        initializerExpression = newChild
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitLocalVariable(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        acceptChildExpressions(visitor, data)
    }

    override fun <D> acceptChildExpressions(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializerExpression?.accept(visitor, data)
    }
}