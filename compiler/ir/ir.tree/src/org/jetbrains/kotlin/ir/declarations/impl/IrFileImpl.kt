/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.name.FqName

class IrFileImpl(
    override var fileEntry: IrFileEntry,
    override val symbol: IrFileSymbol,
    override var packageFqName: FqName
) : IrFile() {
    constructor(
        fileEntry: IrFileEntry,
        packageFragmentDescriptor: PackageFragmentDescriptor
    ) : this(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor), packageFragmentDescriptor.fqName)

    constructor(
        fileEntry: IrFileEntry,
        packageFragmentDescriptor: PackageFragmentDescriptor,
        module: IrModuleFragment,
    ) : this(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor), packageFragmentDescriptor.fqName, module)

    constructor(
        fileEntry: IrFileEntry,
        symbol: IrFileSymbol,
        fqName: FqName,
        module: IrModuleFragment
    ) : this(fileEntry, symbol, fqName) {
        this.module = module
    }

    init {
        symbol.bind(this)
    }

    override lateinit var module: IrModuleFragment

    internal val isInsideModule: Boolean
        get() = ::module.isInitialized

    override val startOffset: Int
        get() = 0

    override val endOffset: Int
        get() = fileEntry.maxOffset

    @UnsafeDuringIrConstructionAPI
    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override var annotations: List<IrConstructorCall> = emptyList()

    override var metadata: MetadataSource? = null
}
