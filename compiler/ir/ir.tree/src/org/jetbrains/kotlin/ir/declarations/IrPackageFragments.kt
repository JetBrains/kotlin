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
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor


@ObsoleteDescriptorBasedAPI
val IrPackageFragment.packageFragmentDescriptor: PackageFragmentDescriptor
    get() = symbol.descriptor

@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrExternalPackageFragment.containerSource: DeserializedContainerSource?
    get() = (symbol.descriptor as? DeserializedMemberDescriptor)?.containerSource

/**
 * This should be a link to [IrModuleFragment] instead.
 *
 * Unfortunately, some package fragments (e.g. some synthetic ones and [IrExternalPackageFragment])
 * are not located in any IR module, but still have a module descriptor.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
val IrPackageFragment.moduleDescriptor: ModuleDescriptor
    get() = if (this is IrFileImpl && isInsideModule) {
        module.descriptor
    } else {
        packageFragmentDescriptor.containingDeclaration
    }

fun createEmptyExternalPackageFragment(module: ModuleDescriptor, fqName: FqName): IrExternalPackageFragment =
    IrExternalPackageFragmentImpl(
        IrExternalPackageFragmentSymbolImpl(EmptyPackageFragmentDescriptor(module, fqName)), fqName
    )