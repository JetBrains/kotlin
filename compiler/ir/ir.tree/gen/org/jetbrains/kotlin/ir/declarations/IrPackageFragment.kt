/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.symbols.IrPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.FqName

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.packageFragment]
 */
abstract class IrPackageFragment : IrElementBase(), IrDeclarationContainer, IrSymbolOwner {
    abstract override val symbol: IrPackageFragmentSymbol

    abstract var packageFqName: FqName

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        declarations.transformInPlace(transformer, data)
    }
}
