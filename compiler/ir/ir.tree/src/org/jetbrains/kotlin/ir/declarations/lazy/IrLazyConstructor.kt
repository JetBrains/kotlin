/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

class IrLazyConstructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrConstructorSymbol,
    name: Name,
    visibility: Visibility,
    isInline: Boolean,
    isExternal: Boolean,
    override val isPrimary: Boolean,
    stubGenerator: DeclarationStubGenerator,
    typeTranslator: TypeTranslator
) :
    IrLazyFunctionBase(
        startOffset, endOffset, origin, name,
        visibility, isInline, isExternal,
        stubGenerator, typeTranslator
    ),
    IrConstructor {

    constructor(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        symbol: IrConstructorSymbol,
        stubGenerator: DeclarationStubGenerator,
        TypeTranslator: TypeTranslator
    ) : this(
        startOffset, endOffset, origin, symbol,
        symbol.descriptor.name,
        symbol.descriptor.visibility,
        symbol.descriptor.isInline,
        symbol.descriptor.isEffectivelyExternal(),
        symbol.descriptor.isPrimary,
        stubGenerator,
        TypeTranslator
    )

    override val typeParameters: MutableList<IrTypeParameter> = arrayListOf()

    init {
        symbol.bind(this)
    }

    override val descriptor: ClassConstructorDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)
}