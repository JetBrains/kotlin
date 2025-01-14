/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.errorDeclaration]
 */
abstract class IrErrorDeclaration : IrDeclarationBase() {
    abstract override val symbol: IrSymbol

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitErrorDeclaration(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitErrorDeclaration(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrElement =
        transformer.visitErrorDeclaration(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrElement =
        transformer.visitErrorDeclaration(this)
}
