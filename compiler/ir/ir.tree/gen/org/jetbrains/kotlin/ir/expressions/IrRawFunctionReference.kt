/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Represents a platform-specific low-level reference to a function.
 *
 * On the JS platform it represents a plain reference to a JavaScript function.
 *
 * On the JVM platform it represents a [java.lang.invoke.MethodHandle] constant.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.rawFunctionReference]
 */
abstract class IrRawFunctionReference : IrDeclarationReference() {
    abstract override var symbol: IrFunctionSymbol

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitRawFunctionReference(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitRawFunctionReference(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpression =
        transformer.visitRawFunctionReference(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrExpression =
        transformer.visitRawFunctionReference(this)
}
