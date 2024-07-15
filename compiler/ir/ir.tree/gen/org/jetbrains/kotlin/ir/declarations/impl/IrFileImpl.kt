/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.name.FqName

class IrFileImpl(
    fileEntry: IrFileEntry,
    symbol: IrFileSymbol,
    packageFqName: FqName,
) : IrFile(
    packageFqName = packageFqName,
    symbol = symbol,
    fileEntry = fileEntry,
) {
    override val startOffset: Int
        get() = 0

    override val endOffset: Int
        get() = fileEntry.maxOffset

    override lateinit var module: IrModuleFragment

    internal val isInsideModule: Boolean
        get() = ::module.isInitialized

    init {
        symbol.bind(this)
    }
}
