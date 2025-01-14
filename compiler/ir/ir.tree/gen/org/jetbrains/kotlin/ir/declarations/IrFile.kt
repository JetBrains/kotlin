/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.file]
 */
abstract class IrFile : IrPackageFragment(), IrMutableAnnotationContainer, IrMetadataSourceOwner {
    abstract override val symbol: IrFileSymbol

    abstract var module: IrModuleFragment

    abstract var fileEntry: IrFileEntry

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitFile(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrFile =
        transformer.visitFile(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrFile =
        transformer.visitFile(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        declarations.forEach { it.acceptVoid(visitor) }
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        declarations.transformInPlace(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        declarations.transformInPlace(transformer)
    }
}
