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
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrAnonymousInitializerSymbol

class IrAnonymousInitializerImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    symbol: IrAnonymousInitializerSymbol,
    isStatic: Boolean,
) : IrAnonymousInitializer(
    startOffset = startOffset,
    endOffset = endOffset,
    origin = origin,
    factory = factory,
    symbol = symbol,
    isStatic = isStatic,
) {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }
}
