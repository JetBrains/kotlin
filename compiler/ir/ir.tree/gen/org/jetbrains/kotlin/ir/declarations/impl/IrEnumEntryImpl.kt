/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.name.Name

class IrEnumEntryImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val factory: IrFactory,
    override var name: Name,
    symbol: IrEnumEntrySymbol,
) : IrEnumEntry(
    startOffset = startOffset,
    endOffset = endOffset,
    origin = origin,
    symbol = symbol,
) {
    override var annotations: List<IrConstructorCall> = emptyList()

    override lateinit var parent: IrDeclarationParent

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override var initializerExpression: IrExpressionBody? = null

    override var correspondingClass: IrClass? = null

    init {
        symbol.bind(this)
    }
}
