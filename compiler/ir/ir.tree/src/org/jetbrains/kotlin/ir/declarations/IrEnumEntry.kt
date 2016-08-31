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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrEnumEntry : IrDeclaration {
    override val declarationKind: IrDeclarationKind get() = IrDeclarationKind.ENUM_ENTRY

    override val descriptor: ClassDescriptor

    var correspondingClass: IrClass?
    var initializerExpression: IrExpression
}

class IrEnumEntryImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: ClassDescriptor
) : IrDeclarationBase(startOffset, endOffset, origin), IrEnumEntry {
    override var correspondingClass: IrClass? = null
        set(value) {
            value?.assertDetached()
            field?.detach()
            field = value
            value?.setTreeLocation(this, ENUM_ENTRY_CLASS_SLOT)
        }

    private var initializerExpressionImpl: IrExpression? = null
    override var initializerExpression: IrExpression
        get() = initializerExpressionImpl!!
        set(value) {
            value.assertDetached()
            initializerExpressionImpl?.detach()
            initializerExpressionImpl = value
            value.setTreeLocation(this, ENUM_ENTRY_INITIALIZER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? {
        return when (slot) {
            ENUM_ENTRY_CLASS_SLOT -> correspondingClass
            ENUM_ENTRY_INITIALIZER_SLOT -> initializerExpression
            else -> null
        }
    }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            ENUM_ENTRY_CLASS_SLOT -> correspondingClass = newChild.assertCast()
            ENUM_ENTRY_INITIALIZER_SLOT -> initializerExpression = newChild.assertCast()
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitEnumEntry(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializerExpression.accept(visitor, data)
        correspondingClass?.accept(visitor, data)
    }
}
