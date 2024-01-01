/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.fieldAccessExpression]
 */
abstract class IrFieldAccessExpression : IrDeclarationReference() {
    abstract override var symbol: IrFieldSymbol

    abstract var superQualifierSymbol: IrClassSymbol?

    var receiver: IrExpression? = null

    abstract var origin: IrStatementOrigin?
}
