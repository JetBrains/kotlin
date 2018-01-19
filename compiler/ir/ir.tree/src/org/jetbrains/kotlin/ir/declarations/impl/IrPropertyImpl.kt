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

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList

class IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val descriptor: PropertyDescriptor,
    override val name: Name,
    override val type: KotlinType,
    override val visibility: Visibility,
    override val modality: Modality,
    override val isVar: Boolean,
    override val isConst: Boolean,
    override val isLateinit: Boolean,
    override val isDelegated: Boolean
) : IrDeclarationBase(startOffset, endOffset, origin),
    IrProperty {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        isDelegated: Boolean,
        descriptor: PropertyDescriptor
    ) : this(
        startOffset, endOffset, origin, descriptor,
        descriptor.name, descriptor.type, descriptor.visibility, descriptor.modality,
        descriptor.isVar, descriptor.isConst, descriptor.isLateInit,
        isDelegated
    )

    constructor(
        startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor
    ) : this(startOffset, endOffset, origin, descriptor.isDelegated, descriptor)

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

    override val typeParameters: MutableList<IrTypeParameter> = SmartList()
    override var backingField: IrField? = null
    override var getter: IrFunction? = null
    override var setter: IrFunction? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitProperty(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        backingField?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters.transform { it.transform(transformer, data) }
        backingField = backingField?.transform(transformer, data) as? IrField
        getter = getter?.transform(transformer, data) as? IrFunction
        setter = setter?.transform(transformer, data) as? IrFunction
    }
}