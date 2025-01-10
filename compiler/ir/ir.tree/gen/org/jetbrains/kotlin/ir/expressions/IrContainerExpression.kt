/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.containerExpression]
 */
abstract class IrContainerExpression : IrExpression(), IrStatementContainer {
    abstract var origin: IrStatementOrigin?

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        statements.transformInPlace(transformer, data)
    }
}
