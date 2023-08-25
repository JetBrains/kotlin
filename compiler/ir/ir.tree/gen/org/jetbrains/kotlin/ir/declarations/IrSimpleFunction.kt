/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.simpleFunction]
 */
abstract class IrSimpleFunction : IrFunction(),
        IrOverridableDeclaration<IrSimpleFunctionSymbol>, IrAttributeContainer {
    abstract override val symbol: IrSimpleFunctionSymbol

    abstract var isTailrec: Boolean

    abstract var isSuspend: Boolean

    abstract override var isFakeOverride: Boolean

    abstract var isOperator: Boolean

    abstract var isInfix: Boolean

    abstract var correspondingPropertySymbol: IrPropertySymbol?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}
