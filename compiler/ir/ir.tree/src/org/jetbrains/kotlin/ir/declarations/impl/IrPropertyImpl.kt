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
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

class IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val descriptor: PropertyDescriptor,
    override val name: Name,
    override val visibility: Visibility,
    override val modality: Modality,
    override val isVar: Boolean,
    override val isConst: Boolean,
    override val isLateinit: Boolean,
    override val isDelegated: Boolean,
    override val isExternal: Boolean
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
        descriptor.name, descriptor.visibility, descriptor.modality,
        isVar = descriptor.isVar,
        isConst = descriptor.isConst,
        isLateinit = descriptor.isLateInit,
        isDelegated = isDelegated,
        isExternal = descriptor.isEffectivelyExternal()
    )

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor
    ) : this(startOffset, endOffset, origin, descriptor.isDelegated, descriptor)

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        isDelegated: Boolean,
        descriptor: PropertyDescriptor,
        backingField: IrField?
    ) : this(startOffset, endOffset, origin, isDelegated, descriptor) {
        this.backingField = backingField
    }

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        isDelegated: Boolean,
        descriptor: PropertyDescriptor,
        backingField: IrField?,
        getter: IrSimpleFunction?,
        setter: IrSimpleFunction?
    ) : this(startOffset, endOffset, origin, isDelegated, descriptor, backingField) {
        this.getter = getter
        this.setter = setter
    }

    override var backingField: IrField? = null
    override var getter: IrSimpleFunction? = null
    override var setter: IrSimpleFunction? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitProperty(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        backingField?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override var metadata: MetadataSource? = null

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        backingField = backingField?.transform(transformer, data) as? IrField
        getter = getter?.run { transform(transformer, data) as IrSimpleFunction }
        setter = setter?.run { transform(transformer, data) as IrSimpleFunction }
    }
}
