/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.ir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.name.FqName

/**
 * Collects the properties of a [IrFile] and builds one.
 *
 * A property with no sensible default is declared `lateinit`, so building without it throws rather than
 * inventing a value. There is no way to express "required" for a property the caller assigns inside a
 * lambda; a constructor parameter could, at the cost of the property being settable only once.
 */
@IrBuilderDsl
class IrFileBuilder {
    lateinit var packageFqName: FqName
    lateinit var module: IrModuleFragment
    var symbol: IrFileSymbol = IrFileSymbolImpl()
    lateinit var fileEntry: IrFileEntry
    var metadata: MetadataSource? = null

    @PublishedApi
    internal fun build(): IrFile {
        val result = IrFileImpl(
            packageFqName = packageFqName,
            module = module,
            symbol = symbol,
            fileEntry = fileEntry,
        )
        result.metadata = metadata
        return result
    }


    /**
     * Copies the properties of [from] that identify a *kind* of IrFile, not a particular one,
     * including its source range.
     *
     * Its symbol, parent and children are left alone: sharing a symbol between two declarations would bind both
     * to one, and sharing a body or a receiver would splice one tree into another. Properties the element
     * declares `lateinit` are skipped too, since reading one that was never set throws.
     */
    fun updateFrom(from: IrFile) {
        packageFqName = from.packageFqName
        fileEntry = from.fileEntry
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildFile(init: IrFileBuilder.() -> Unit): IrFile {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return IrFileBuilder().apply(init).build()
}
