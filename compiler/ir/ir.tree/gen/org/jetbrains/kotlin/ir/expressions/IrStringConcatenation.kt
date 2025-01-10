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
 * Represents a string template expression.
 *
 * For example, the value of `template` in the following code:
 * ```kotlin
 * val i = 10
 * val template = "i = $i"
 * ```
 * will be represented by [IrStringConcatenation] with the following list of [arguments]:
 * - [IrConst] whose `value` is `"i = "`
 * - [IrGetValue] whose `symbol` will be that of the `i` variable. 
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.stringConcatenation]
 */
abstract class IrStringConcatenation : IrExpression() {
    abstract val arguments: MutableList<IrExpression>

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitStringConcatenation(this, data)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        arguments.transformInPlace(transformer, data)
    }
}
