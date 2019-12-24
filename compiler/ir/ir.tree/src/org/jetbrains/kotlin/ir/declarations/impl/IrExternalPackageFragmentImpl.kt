/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName

class IrExternalPackageFragmentImpl(
    override val symbol: IrExternalPackageFragmentSymbol,
    override val fqName: FqName
) : IrElementBase(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
    IrExternalPackageFragment {

    init {
        symbol.bind(this)
    }

    override val packageFragmentDescriptor: PackageFragmentDescriptor get() = symbol.descriptor

    override val declarations: MutableList<IrDeclaration> = ArrayList()

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitExternalPackageFragment(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        declarations.forEachIndexed { i, irDeclaration ->
            declarations[i] = irDeclaration.transform(transformer, data) as IrDeclaration
        }
    }
}

fun IrExternalPackageFragmentImpl(symbol: IrExternalPackageFragmentSymbol) =
    IrExternalPackageFragmentImpl(symbol, symbol.descriptor.fqName)
