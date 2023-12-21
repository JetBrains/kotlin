/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirFieldSymbol
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.fieldAccessExpression]
 */
abstract class BirFieldAccessExpression : BirDeclarationReference(), BirElement {
    abstract override var symbol: BirFieldSymbol
    abstract var superQualifierSymbol: BirClassSymbol?
    abstract var receiver: BirExpression?
    abstract var origin: IrStatementOrigin?

    companion object : BirElementClass(BirFieldAccessExpression::class.java, 46, false)
}
