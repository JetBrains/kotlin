/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

class IrConstructorImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrConstructorSymbol,
    name: Name,
    visibility: Visibility,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    override val isPrimary: Boolean,
    isExpect: Boolean
) :
    IrFunctionBase(
        startOffset, endOffset, origin, name,
        visibility,
        isInline, isExternal, isExpect,
        returnType
    ),
    IrConstructor {

    init {
        symbol.bind(this)
    }

    override val descriptor: ClassConstructorDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)
}

fun IrConstructorImpl(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    symbol: IrConstructorSymbol,
    returnType: IrType,
    body: IrBody? = null
) =
    IrConstructorImpl(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        symbol.descriptor.visibility,
        returnType,
        isInline = symbol.descriptor.isInline,
        isExternal = symbol.descriptor.isEffectivelyExternal(),
        isPrimary = symbol.descriptor.isPrimary,
        isExpect = symbol.descriptor.isExpect
    ).apply {
        this.body = body
    }