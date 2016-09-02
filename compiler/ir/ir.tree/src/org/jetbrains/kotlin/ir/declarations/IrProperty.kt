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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrProperty : IrDeclaration {
    override val descriptor: PropertyDescriptor
    var getter: IrPropertyGetter?
    var setter: IrPropertySetter?

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.PROPERTY
}

interface IrSimpleProperty : IrProperty {
    var initializer: IrBody?
}

interface IrDelegatedProperty : IrProperty {
    var delegate: IrSimpleProperty
}

abstract class IrPropertyBase(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: PropertyDescriptor
) : IrDeclarationBase(startOffset, endOffset, origin), IrProperty {
    override var getter: IrPropertyGetter? = null
        set(newGetter) {
            newGetter?.assertDetached()
            field?.detach()
            field = newGetter
            newGetter?.setTreeLocation(this, PROPERTY_GETTER_SLOT)
        }

    override var setter: IrPropertySetter? = null
        set(newSetter) {
            newSetter?.assertDetached()
            field?.detach()
            field = newSetter
            newSetter?.setTreeLocation(this, PROPERTY_SETTER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                PROPERTY_GETTER_SLOT -> getter
                PROPERTY_SETTER_SLOT -> setter
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            PROPERTY_GETTER_SLOT -> getter = newChild.assertCast()
            PROPERTY_SETTER_SLOT -> setter = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }
}

class IrSimplePropertyImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        valueInitializer: IrBody? = null
) : IrPropertyBase(startOffset, endOffset, origin, descriptor), IrSimpleProperty {
    override var initializer: IrBody? = valueInitializer
        set(value) {
            value?.assertDetached()
            field?.detach()
            field = value
            value?.setTreeLocation(this, INITIALIZER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                INITIALIZER_SLOT -> initializer
                else -> super.getChild(slot)
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            INITIALIZER_SLOT -> initializer = newChild.assertCast()
            else -> super.replaceChild(slot, newChild)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitSimpleProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }
}

class IrDelegatedPropertyImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor
) : IrPropertyBase(startOffset, endOffset, origin, descriptor), IrDelegatedProperty {
    constructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            descriptor: PropertyDescriptor,
            delegate: IrSimpleProperty
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.delegate = delegate
    }

    private var delegateImpl: IrSimpleProperty? = null
    override var delegate: IrSimpleProperty
        get() = delegateImpl!!
        set(value) {
            value.assertDetached()
            delegateImpl?.detach()
            delegateImpl = value
            value.setTreeLocation(this, DELEGATE_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                DELEGATE_SLOT -> delegate
                else -> super.getChild(slot)
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            DELEGATE_SLOT -> delegate = newChild.assertCast()
            else -> super.replaceChild(slot, newChild)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitDelegatedProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        delegate.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }
}
