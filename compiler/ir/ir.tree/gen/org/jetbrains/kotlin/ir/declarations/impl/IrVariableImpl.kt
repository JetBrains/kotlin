/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator
import org.jetbrains.kotlin.name.Name

class IrVariableImpl internal constructor(
    @Suppress("UNUSED_PARAMETER") constructorIndicator: IrElementConstructorIndicator?,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    type: IrType,
    symbol: IrVariableSymbol,
    isVar: Boolean,
    isConst: Boolean,
    isLateinit: Boolean,
) : IrVariable(
    startOffset = startOffset,
    endOffset = endOffset,
    origin = origin,
    name = name,
    type = type,
    symbol = symbol,
    isVar = isVar,
    isConst = isConst,
    isLateinit = isLateinit,
) {
    override val factory: IrFactory
        get() = error("Create IrVariableImpl directly")

    @ObsoleteDescriptorBasedAPI
    override val descriptor: VariableDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }
}
