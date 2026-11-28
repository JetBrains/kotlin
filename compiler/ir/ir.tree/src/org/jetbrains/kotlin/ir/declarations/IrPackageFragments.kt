/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.name.FqName


@ObsoleteDescriptorBasedAPI
val IrPackageFragment.packageFragmentDescriptor: PackageFragmentDescriptor
    get() = symbol.descriptor

val IrPackageFragment.moduleDescriptor: ModuleDescriptor
    get() = module.descriptor


fun createEmptyExternalPackageFragment(module: IrModuleFragment, fqName: FqName): IrExternalPackageFragment =
    IrExternalPackageFragmentImpl(
        IrExternalPackageFragmentSymbolImpl(EmptyPackageFragmentDescriptor(module.descriptor, fqName)), fqName, module
    )
