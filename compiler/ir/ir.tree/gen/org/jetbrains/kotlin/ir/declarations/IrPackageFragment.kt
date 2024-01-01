/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrPackageFragmentSymbol
import org.jetbrains.kotlin.name.FqName

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.packageFragment]
 */
abstract class IrPackageFragment : IrElementBase(), IrDeclarationContainer, IrSymbolOwner {
    abstract override val symbol: IrPackageFragmentSymbol

    @ObsoleteDescriptorBasedAPI
    abstract val packageFragmentDescriptor: PackageFragmentDescriptor

    /**
     * This should be a link to [IrModuleFragment] instead. 
     *
     * Unfortunately, some package fragments (e.g. some synthetic ones and [IrExternalPackageFragment])
     * are not located in any IR module, but still have a module descriptor. 
     */
    abstract val moduleDescriptor: ModuleDescriptor

    abstract var packageFqName: FqName

    @Deprecated(
        message = "Please use `packageFqName` instead",
        replaceWith = ReplaceWith("packageFqName"),
        level = DeprecationLevel.ERROR,
    )
    var fqName: FqName
        get() = packageFqName
        set(value) {
            packageFqName = value
        }
}
