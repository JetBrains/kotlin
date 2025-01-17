/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.inlinedFunctionBlock]
 */
abstract class IrInlinedFunctionBlock : IrBlock() {
    /**
     * Represents the start offset of the inlined function that was located inside `fileEntry`.
     */
    abstract var inlinedFunctionStartOffset: Int

    /**
     * Represents the end offset of the inlined function that was located inside `fileEntry`.                
     */
    abstract var inlinedFunctionEndOffset: Int

    abstract var inlinedFunctionSymbol: IrFunctionSymbol?

    abstract var inlinedFunctionFileEntry: IrFileEntry

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitInlinedFunctionBlock(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitInlinedFunctionBlock(this)
    }
}
