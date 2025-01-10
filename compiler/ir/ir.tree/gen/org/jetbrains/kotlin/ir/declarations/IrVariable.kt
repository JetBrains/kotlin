/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.variable]
 */
abstract class IrVariable : IrDeclarationBase(), IrValueDeclaration {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: VariableDescriptor

    abstract override val symbol: IrVariableSymbol

    abstract var isVar: Boolean

    abstract var isConst: Boolean

    abstract var isLateinit: Boolean

    abstract var initializer: IrExpression?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitVariable(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitVariable(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrElement =
        transformer.visitVariable(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrElement =
        transformer.visitVariable(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        initializer?.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        initializer = initializer?.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        initializer = initializer?.transformVoid(transformer)
    }
}
