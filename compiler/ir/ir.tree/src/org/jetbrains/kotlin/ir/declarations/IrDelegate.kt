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
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrDelegate : IrDeclaration {
    override val descriptor: VariableDescriptor

    var initializer: IrExpressionBody

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.DELEGATE
}

class IrDelegateImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: VariableDescriptor
) : IrDeclarationBase(startOffset, endOffset, origin), IrDelegate {
    constructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            descriptor: VariableDescriptor,
            initializer: IrExpressionBody
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.initializer = initializer
    }

    private var initializerImpl: IrExpressionBody? = null
    override var initializer: IrExpressionBody
        get() = initializerImpl!!
        set(value) {
            value.assertDetached()
            initializerImpl?.detach()
            initializerImpl = value
            value.setTreeLocation(this, INITIALIZER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                INITIALIZER_SLOT -> initializer
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            INITIALIZER_SLOT -> initializer = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitDelegate(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
    }
}