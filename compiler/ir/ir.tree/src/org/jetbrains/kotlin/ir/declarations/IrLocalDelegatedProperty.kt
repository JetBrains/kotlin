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

import org.jetbrains.kotlin.descriptors.VariableAccessorDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrLocalDelegatedProperty : IrDeclaration {
    override val descriptor: VariableDescriptorWithAccessors
    var delegate: IrVariable
    var getter: IrLocalPropertyAccessor
    var setter: IrLocalPropertyAccessor?

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.LOCAL_PROPERTY
}

interface IrLocalPropertyAccessor : IrGeneralFunction {
    override val descriptor: VariableAccessorDescriptor

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.LOCAL_PROPERTY_ACCESSOR
}

class IrLocalDelegatedPropertyImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: VariableDescriptorWithAccessors
) : IrDeclarationBase(startOffset, endOffset, origin), IrLocalDelegatedProperty {
    constructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            descriptor: VariableDescriptorWithAccessors,
            delegate: IrVariable
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.delegate = delegate
    }

    private var delegateImpl: IrVariable? = null
    override var delegate: IrVariable
        get() = delegateImpl!!
        set(value) {
            value.assertDetached()
            delegateImpl?.detach()
            delegateImpl = value
            value.setTreeLocation(this, DELEGATE_SLOT)
        }

    private var getterImpl: IrLocalPropertyAccessor? = null
    override var getter: IrLocalPropertyAccessor
        get() = getterImpl!!
        set(value) {
            value.assertDetached()
            getterImpl?.detach()
            getterImpl = value
            value.setTreeLocation(this, PROPERTY_GETTER_SLOT)
        }

    override var setter: IrLocalPropertyAccessor? = null
        set(value) {
            value?.assertDetached()
            field?.detach()
            field = value
            value?.setTreeLocation(this, PROPERTY_SETTER_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                DELEGATE_SLOT -> delegate
                PROPERTY_GETTER_SLOT -> getter
                PROPERTY_SETTER_SLOT -> setter
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            DELEGATE_SLOT -> delegate = newChild.assertCast()
            PROPERTY_GETTER_SLOT -> getter = newChild.assertCast()
            PROPERTY_SETTER_SLOT -> setter = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitLocalDelegatedProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        delegate.accept(visitor, data)
        getter.accept(visitor, data)
        setter?.accept(visitor, data)
    }
}

class IrLocalPropertyAccessorImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: VariableAccessorDescriptor
) : IrGeneralFunctionBase(startOffset, endOffset, origin), IrLocalPropertyAccessor {
    constructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            descriptor: VariableAccessorDescriptor,
            body: IrBody
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.body = body
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitLocalPropertyAccessor(this, data)
    }
}
