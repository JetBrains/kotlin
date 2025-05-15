/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import unwrap
import wrap

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.expression]
 */
abstract class IrExpression : IrElementBase(), IrStatement, IrVarargElement {
    protected abstract var _type: IrType

    var type: IrType
        get() = if (this is IrCallableReference<*>) _type else _type.wrap()
        set(value) {
            _type = value.unwrap()
        }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrExpression =
        accept(transformer, data) as IrExpression
}