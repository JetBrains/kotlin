/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.builders.expressions.v2

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

abstract class IrLoopBuilder internal constructor() : IrExpressionBuilder() {
    var origin: IrStatementOrigin? = null

    var body: IrExpression? = null

    lateinit var condition: IrExpression

    var label: String? = null
}
