/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.builders.expressions.v2

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType

/**
 * The arguments of a call have one slot per parameter of the callee, as [org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression.arguments]:
 * the slots of a function call exist when the lambda runs, and are filled by index. `build` checks that their number wasn't changed.
 */
abstract class IrMemberAccessExpressionBuilder<S : IrSymbol> internal constructor() : IrDeclarationReferenceBuilder() {
    abstract override val symbol: S

    var origin: IrStatementOrigin? = null

    val arguments: MutableList<IrExpression?> = []

    val typeArguments: MutableList<IrType?> = []
}
