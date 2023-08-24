/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerShallow
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorShallow

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.containerExpression]
 */
abstract class IrContainerExpression : IrExpression(), IrStatementContainer {
    abstract var origin: IrStatementOrigin?

    override val statements: MutableList<IrStatement> = ArrayList(2)

    override fun <D> acceptChildren(visitor: IrElementVisitorShallow<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformerShallow<D>,
            data: D) {
        statements.transformInPlace(transformer, data)
    }
}
