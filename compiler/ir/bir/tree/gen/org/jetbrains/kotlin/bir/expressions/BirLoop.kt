/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

abstract class BirLoop() : BirExpression() {
    abstract var origin: IrStatementOrigin?

    abstract var body: BirExpression?

    abstract var condition: BirExpression

    abstract var label: String?

    companion object : BirElementClass<BirLoop>(BirLoop::class.java, 63, false)
}
