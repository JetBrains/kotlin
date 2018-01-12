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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class IrLocalDelegatedPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val descriptor: VariableDescriptorWithAccessors,
    override val name: Name,
    override val type: KotlinType,
    override val isVar: Boolean
) : IrDeclarationBase(startOffset, endOffset, origin),
    IrLocalDelegatedProperty {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors
    ) : this(
        startOffset, endOffset, origin, descriptor,
        descriptor.name, descriptor.type, descriptor.isVar
    )

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors,
        delegate: IrVariable
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.delegate = delegate
    }

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: VariableDescriptorWithAccessors,
        delegate: IrVariable,
        getter: IrFunction,
        setter: IrFunction?
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.delegate = delegate
        this.getter = getter
        this.setter = setter
    }

    override lateinit var delegate: IrVariable
    override lateinit var getter: IrFunction
    override var setter: IrFunction? = null

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