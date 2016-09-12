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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrPropertyImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val isDelegated: Boolean,
        override val descriptor: PropertyDescriptor
) : IrDeclarationBase(startOffset, endOffset, origin), IrProperty {
    constructor(
            startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, isDelegated: Boolean, descriptor: PropertyDescriptor,
            backingField: IrField?
    ) : this(startOffset, endOffset, origin, isDelegated, descriptor) {
        this.backingField = backingField
    }

    constructor(
            startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, isDelegated: Boolean, descriptor: PropertyDescriptor,
            backingField: IrField?, getter: IrFunction?, setter: IrFunction?
    ) : this(startOffset, endOffset, origin, isDelegated, descriptor, backingField) {
        this.getter = getter
        this.setter = setter
    }

    override var backingField: IrField? = null
        set(value) {
            field?.detach()
            field = value
            value?.setTreeLocation(this, BACKING_FIELD_SLOT)
        }

    override var getter: IrFunction? = null
        set(value) {
            field?.detach()
            field = value
            value?.setTreeLocation(this, PROPERTY_GETTER_SLOT)
        }

    override var setter: IrFunction? = null
        set(value) {
            field?.detach()
            field = value
            value?.setTreeLocation(this, PROPERTY_SETTER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                BACKING_FIELD_SLOT -> backingField
                PROPERTY_GETTER_SLOT -> getter
                PROPERTY_SETTER_SLOT -> setter
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            BACKING_FIELD_SLOT -> backingField = newChild.assertCast()
            PROPERTY_GETTER_SLOT -> getter = newChild.assertCast()
            PROPERTY_SETTER_SLOT -> setter = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitProperty(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        backingField?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }
}