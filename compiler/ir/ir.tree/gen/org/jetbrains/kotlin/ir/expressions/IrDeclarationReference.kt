/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.parentAsClass

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.declarationReference]
 */
abstract class IrDeclarationReference(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
) : IrExpression(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
) {
    abstract val symbol: IrSymbol
}
