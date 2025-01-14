/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformerVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.expression]
 */
abstract class IrExpression : IrElementBase(), IrStatement, IrVarargElement {
    abstract var type: IrType

    abstract override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrExpression

    abstract override fun transformVoid(transformer: IrLeafTransformerVoid): IrExpression
}
