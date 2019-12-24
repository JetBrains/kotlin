/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

@Suppress("DEPRECATION_ERROR")
class IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrPropertySymbol,
    override val name: Name = symbol.descriptor.name,
    override val visibility: Visibility = symbol.descriptor.visibility,
    override val modality: Modality = symbol.descriptor.modality,
    override val isVar: Boolean = symbol.descriptor.isVar,
    override val isConst: Boolean = symbol.descriptor.isConst,
    override val isLateinit: Boolean = symbol.descriptor.isLateInit,
    override val isDelegated: Boolean = @Suppress("DEPRECATION") symbol.descriptor.isDelegated,
    override val isExternal: Boolean = symbol.descriptor.isEffectivelyExternal(),
    override val isExpect: Boolean = symbol.descriptor.isExpect,
    override val isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE
) : IrDeclarationBase(startOffset, endOffset, origin),
    IrProperty {

    init {
        symbol.bind(this)
    }

    override val descriptor: PropertyDescriptor = symbol.descriptor

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

@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: PropertyDescriptor,
    name: Name,
    visibility: Visibility,
    modality: Modality,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
    isDelegated: Boolean,
    isExternal: Boolean
) =
    IrPropertyImpl(
        startOffset, endOffset, origin,
        IrPropertySymbolImpl(descriptor),
        name, visibility, modality,
        isVar = isVar,
        isConst = isConst,
        isLateinit = isLateinit,
        isDelegated = isDelegated,
        isExternal = isExternal
    )

@Suppress("DEPRECATION_ERROR")
@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    isDelegated: Boolean,
    descriptor: PropertyDescriptor
) =
    IrPropertyImpl(
        startOffset, endOffset, origin, descriptor,
        descriptor.name, descriptor.visibility, descriptor.modality,
        isVar = descriptor.isVar,
        isConst = descriptor.isConst,
        isLateinit = descriptor.isLateInit,
        isDelegated = isDelegated,
        isExternal = descriptor.isEffectivelyExternal()
    )

@Suppress("DEPRECATION", "DEPRECATION_ERROR")
@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: PropertyDescriptor
) =
    IrPropertyImpl(startOffset, endOffset, origin, descriptor.isDelegated, descriptor)

@Suppress("DEPRECATION_ERROR")
@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    isDelegated: Boolean,
    descriptor: PropertyDescriptor,
    backingField: IrField?
) =
    IrPropertyImpl(startOffset, endOffset, origin, isDelegated, descriptor).apply {
        this.backingField = backingField
    }

@Suppress("DEPRECATION_ERROR")
@Deprecated(message = "Don't use descriptor-based API for IrProperty", level = DeprecationLevel.ERROR)
fun IrPropertyImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    isDelegated: Boolean,
    descriptor: PropertyDescriptor,
    backingField: IrField?,
    getter: IrSimpleFunction?,
    setter: IrSimpleFunction?
) =
    IrPropertyImpl(startOffset, endOffset, origin, isDelegated, descriptor, backingField).apply {
        this.getter = getter
        this.setter = setter
    }
