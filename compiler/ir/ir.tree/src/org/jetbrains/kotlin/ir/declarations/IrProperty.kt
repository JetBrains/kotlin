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

    fun <D> acceptAccessors(visitor: IrElementVisitor<Unit, D>, data: D)
}

interface IrSimpleProperty : IrProperty {
    var valueInitializer: IrBody?
}

interface IrDelegatedProperty : IrProperty {
    var delegateInitializer: IrBody
}

// TODO synchronization?
abstract class IrPropertyBase(
        startOffset: Int,
        endOffset: Int,
        originKind: IrDeclarationOriginKind,
        override val descriptor: PropertyDescriptor
) : IrDeclarationBase(startOffset, endOffset, originKind), IrProperty {
    override var getter: IrPropertyGetter? = null
        set(newGetter) {
            newGetter?.run { assert(property == null) { "$newGetter: should not have a property" } }
            newGetter?.property = this
            field = newGetter
        }

    override var setter: IrPropertySetter? = null
        set(newSetter) {
            newSetter?.run { assert(property == null) { "$newSetter: should not have a property" } }
            newSetter?.property = this
            field = newSetter
        }

    override fun <D> acceptAccessors(visitor: IrElementVisitor<Unit, D>, data: D) {
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }
}

class IrSimplePropertyImpl(
        startOffset: Int,
        endOffset: Int,
        originKind: IrDeclarationOriginKind,
        descriptor: PropertyDescriptor,
        valueInitializer: IrBody? = null
) : IrPropertyBase(startOffset, endOffset, originKind, descriptor), IrSimpleProperty {
    override var valueInitializer: IrBody? = valueInitializer
        set(value) {
            value?.assertDetached()
            field?.detach()
            field = value
            value?.setTreeLocation(this, INITIALIZER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                INITIALIZER_SLOT -> valueInitializer
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            INITIALIZER_SLOT -> valueInitializer = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitSimpleProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        valueInitializer?.accept(visitor, data)
    }
}

class IrDelegatedPropertyImpl(
        startOffset: Int,
        endOffset: Int,
        originKind: IrDeclarationOriginKind,
        descriptor: PropertyDescriptor,
        delegateInitializer: IrBody
) : IrPropertyBase(startOffset, endOffset, originKind, descriptor), IrDelegatedProperty {
    override var delegateInitializer: IrBody = delegateInitializer
        set(value) {
            value.assertDetached()
            field.detach()
            field = value
            value.setTreeLocation(this, INITIALIZER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                INITIALIZER_SLOT -> delegateInitializer
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            INITIALIZER_SLOT -> delegateInitializer = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitDelegatedProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        delegateInitializer.accept(visitor, data)
    }
}
