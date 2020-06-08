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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.carriers.PropertyCarrier
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

abstract class IrPropertyCommonImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val name: Name,
    override val visibility: Visibility,
    override val modality: Modality,
    override val isVar: Boolean,
    override val isConst: Boolean,
    override val isLateinit: Boolean,
    override val isDelegated: Boolean,
    override val isExternal: Boolean,
    override val isExpect: Boolean,
    override val isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE
) : IrDeclarationBase<PropertyCarrier>(startOffset, endOffset, origin),
    IrProperty,
    PropertyCarrier {

    @DescriptorBasedIr
    abstract override val descriptor: PropertyDescriptor

    override var backingFieldField: IrField? = null

    override var backingField: IrField?
        get() = getCarrier().backingFieldField
        set(v) {
            if (backingField !== v) {
                setCarrier().backingFieldField = v
            }
        }

    override var getterField: IrSimpleFunction? = null

    override var getter: IrSimpleFunction?
        get() = getCarrier().getterField
        set(v) {
            if (getter !== v) {
                setCarrier().getterField = v
            }
        }

    override var setterField: IrSimpleFunction? = null

    override var setter: IrSimpleFunction?
        get() = getCarrier().setterField
        set(v) {
            if (setter !== v) {
                setCarrier().setterField = v
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

    override var metadataField: MetadataSource? = null

    override var metadata: MetadataSource?
        get() = getCarrier().metadataField
        set(v) {
            if (metadata !== v) {
                setCarrier().metadataField = v
            }
        }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        backingField = backingField?.transform(transformer, data) as? IrField
        getter = getter?.run { transform(transformer, data) as IrSimpleFunction }
        setter = setter?.run { transform(transformer, data) as IrSimpleFunction }
    }
}

class IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrPropertySymbol,
    override val name: Name,
    override val visibility: Visibility,
    override val modality: Modality,
    override val isVar: Boolean,
    override val isConst: Boolean,
    override val isLateinit: Boolean,
    override val isDelegated: Boolean,
    override val isExternal: Boolean,
    override val isExpect: Boolean = false,
    override val isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE
) : IrPropertyCommonImpl(startOffset, endOffset, origin, name, visibility, modality, isVar, isConst, isLateinit, isDelegated, isExternal, isExpect, isFakeOverride) {

    @Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.WARNING)
    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor,
        symbol: IrPropertySymbol = IrPropertySymbolImpl(descriptor),
        name: Name = descriptor.name,
        visibility: Visibility = descriptor.visibility,
        modality: Modality = descriptor.modality,
        isVar: Boolean = descriptor.isVar,
        isConst: Boolean = descriptor.isConst,
        isLateinit: Boolean = descriptor.isLateInit,
        isDelegated: Boolean = descriptor.isDelegated,
        isExternal: Boolean = descriptor.isExternal
    ) : this(
        startOffset, endOffset, origin,
        symbol,
        name, visibility, modality,
        isVar = isVar,
        isConst = isConst,
        isLateinit = isLateinit,
        isDelegated = isDelegated,
        isExternal = isExternal,
        isExpect = descriptor.isExpect
    )

    @Suppress("DEPRECATION")
    @Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.WARNING)
    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        isDelegated: Boolean,
        descriptor: PropertyDescriptor
    ) : this(
        startOffset, endOffset, origin, descriptor,
        name = descriptor.name,
        visibility = descriptor.visibility,
        modality = descriptor.modality,
        isVar = descriptor.isVar,
        isConst = descriptor.isConst,
        isLateinit = descriptor.isLateInit,
        isDelegated = isDelegated,
        isExternal = descriptor.isEffectivelyExternal()
    )

    @Suppress("DEPRECATION")
    @Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.WARNING)
    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        descriptor: PropertyDescriptor
    ) : this(startOffset, endOffset, origin, descriptor.isDelegated, descriptor)

    @Suppress("DEPRECATION")
    @Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.WARNING)
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

    @Suppress("DEPRECATION")
    @Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.WARNING)
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

    init {
        symbol.bind(this)
    }

    @DescriptorBasedIr
    override val descriptor: PropertyDescriptor = symbol.descriptor
}

class IrFakeOverridePropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    override var visibility: Visibility,
    override var modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean,
    isExpect: Boolean,
) : IrPropertyCommonImpl(startOffset, endOffset, origin, name, visibility, modality, isVar, isConst, isLateinit,
    isDelegated, isExternal, isExpect, isFakeOverride = true)
{
    private var _symbol: IrPropertySymbol? = null

    override val symbol: IrPropertySymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    @DescriptorBasedIr
    override val descriptor
        get() = _symbol?.descriptor ?: WrappedPropertyDescriptor()

    @OptIn(DescriptorBasedIr::class)
    fun acquireSymbol(symbol: IrPropertySymbol) {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        (symbol.descriptor as? WrappedPropertyDescriptor)?.bind(this)
    }
}
