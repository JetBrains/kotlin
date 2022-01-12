/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrPackageFragmentSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName

abstract class IrPackageFragment : IrElementBase(), IrDeclarationContainer, IrSymbolOwner {
    @ObsoleteDescriptorBasedAPI
    abstract val packageFragmentDescriptor: PackageFragmentDescriptor
    abstract override val symbol: IrPackageFragmentSymbol

    abstract val fqName: FqName

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitPackageFragment(this, data)
}
