/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.name.FqName

class IrExternalPackageFragmentImpl(
    symbol: IrExternalPackageFragmentSymbol,
    packageFqName: FqName,
) : IrExternalPackageFragment(
    packageFqName = packageFqName,
    symbol = symbol,
) {
    override val startOffset: Int
        get() = UNDEFINED_OFFSET

    override val endOffset: Int
        get() = UNDEFINED_OFFSET

    companion object {
        @Deprecated(
            message = "Use org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment instead",
            replaceWith = ReplaceWith("createEmptyExternalPackageFragment", "org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment")
        )
        fun createEmptyExternalPackageFragment(module: ModuleDescriptor, fqName: FqName): IrExternalPackageFragment =
            org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment(module, fqName)
    }

    init {
        symbol.bind(this)
    }
}
