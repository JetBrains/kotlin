/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal


class IrFieldImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrFieldSymbol,
    override val name: Name,
    override val type: IrType,
    override val visibility: Visibility,
    override val isFinal: Boolean,
    override val isExternal: Boolean,
    override val isStatic: Boolean,
    override val isFakeOverride: Boolean
) : IrDeclarationBase(startOffset, endOffset, origin),
    IrField {

    init {
        symbol.bind(this)
    }

    override val descriptor: PropertyDescriptor = symbol.descriptor

    override var initializer: IrExpressionBody? = null

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override val overriddenSymbols: MutableList<IrFieldSymbol> = mutableListOf()

    override var metadata: MetadataSource.Property? = null

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitField(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        initializer = initializer?.transform(transformer, data)
    }
}

fun IrFieldImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrFieldSymbol,
    type: IrType,
    visibility: Visibility = symbol.descriptor.visibility
) =
    IrFieldImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name, type, visibility,
        isFinal = !symbol.descriptor.isVar,
        isExternal = symbol.descriptor.isEffectivelyExternal(),
        isStatic = symbol.descriptor.dispatchReceiverParameter == null,
        isFakeOverride = origin == IrDeclarationOrigin.FAKE_OVERRIDE
    )

fun IrFieldImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: PropertyDescriptor,
    type: IrType
): IrFieldImpl =
    IrFieldImpl(startOffset, endOffset, origin, IrFieldSymbolImpl(descriptor), type)
