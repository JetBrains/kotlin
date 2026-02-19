/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.name.FqName

fun IrFileImpl(
    fileEntry: IrFileEntry,
    symbol: IrFileSymbol,
    fqName: FqName,
    module: IrModuleFragment,
) = IrFileImpl(fileEntry, symbol, fqName).apply {
    this.module = module
}

fun IrFileImpl(
    fileEntry: IrFileEntry,
    packageFragmentDescriptor: PackageFragmentDescriptor,
) = IrFileImpl(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor), packageFragmentDescriptor.fqName)

fun IrFileImpl(
    fileEntry: IrFileEntry,
    packageFragmentDescriptor: PackageFragmentDescriptor,
    module: IrModuleFragment,
) = IrFileImpl(fileEntry, IrFileSymbolImpl(packageFragmentDescriptor), packageFragmentDescriptor.fqName, module)
