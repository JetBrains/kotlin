/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.model.AnnotationMarker

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.constructorCall]
 */
abstract class IrConstructorCall(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    valueArguments: Array<IrExpression?>,
    typeArguments: Array<IrType?>,
    override var symbol: IrConstructorSymbol,
    var source: SourceElement,
    var constructorTypeArgumentsCount: Int,
) : IrFunctionAccessExpression(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
    valueArguments = valueArguments,
    typeArguments = typeArguments,
), AnnotationMarker {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstructorCall(this, data)
}
