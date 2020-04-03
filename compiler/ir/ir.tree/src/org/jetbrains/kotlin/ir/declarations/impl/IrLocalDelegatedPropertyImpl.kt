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

import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.carriers.LocalDelegatedPropertyCarrier
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

// TODO make not persistent
class IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrLocalDelegatedPropertySymbol,
    override val name: Name,
    override val type: IrType,
    override val isVar: Boolean
) :
    IrDeclarationBase<LocalDelegatedPropertyCarrier>(startOffset, endOffset, origin),
    IrLocalDelegatedProperty,
    LocalDelegatedPropertyCarrier {

    init {
        symbol.bind(this)
    }

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrLocalDelegatedPropertySymbol,
        type: IrType
    ) : this(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        type,
        symbol.descriptor.isVar
    )

    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors,
        name: Name,
        type: IrType,
        isVar: Boolean
    ) : this(
        startOffset, endOffset, origin,
        IrLocalDelegatedPropertySymbolImpl(descriptor),
        name, type, isVar
    )

    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors,
        type: IrType
    ) : this(
        startOffset, endOffset, origin,
        IrLocalDelegatedPropertySymbolImpl(descriptor),
        descriptor.name, type, descriptor.isVar
    )

    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors,
        type: IrType,
        delegate: IrVariable,
        getter: IrFunction,
        setter: IrFunction?
    ) : this(startOffset, endOffset, origin, descriptor, type) {
        this.delegate = delegate
        this.getter = getter
        this.setter = setter
    }

    override val descriptor: VariableDescriptorWithAccessors
        get() = symbol.descriptor

    override var delegateField: IrVariable? = null

    override var delegate: IrVariable
        get() = getCarrier().delegateField!!
        set(v) {
            if (getCarrier().delegateField !== v) {
                setCarrier().delegateField = v
            }
        }

    override var getterField: IrFunction? = null

    override var getter: IrFunction
        get() = getCarrier().getterField!!
        set(v) {
            if (getCarrier().getterField !== v) {
                setCarrier().getterField = v
            }
        }

    override var setterField: IrFunction? = null

    override var setter: IrFunction?
        get() = getCarrier().setterField
        set(v) {
            if (setter !== v) {
                setCarrier().setterField = v
            }
        }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitLocalDelegatedProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        delegate.accept(visitor, data)
        getter.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        delegate = delegate.transform(transformer, data) as IrVariable
        getter = getter.transform(transformer, data) as IrFunction
        setter = setter?.transform(transformer, data) as? IrFunction
    }
}