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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType

interface IrInitializeProperty : IrDeclarationReference {
    override val descriptor: PropertyDescriptor

    var initBlockExpression: IrExpression?
}

class IrInitializePropertyImpl(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        descriptor: PropertyDescriptor
) : IrDeclarationReferenceBase<PropertyDescriptor>(startOffset, endOffset, type, descriptor), IrInitializeProperty {
    override var initBlockExpression: IrExpression? = null
        set(value) {
            value?.assertDetached()
            field?.detach()
            field = value
            value?.setTreeLocation(this, CHILD_EXPRESSION_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                CHILD_EXPRESSION_SLOT -> initBlockExpression
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            CHILD_EXPRESSION_SLOT -> initBlockExpression = newChild.assertCast()
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitInitializeProperty(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initBlockExpression?.accept(visitor, data)
    }
}