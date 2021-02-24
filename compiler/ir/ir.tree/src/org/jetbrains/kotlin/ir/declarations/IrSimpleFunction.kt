/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrSimpleFunction :
    IrFunction(),
    IrOverridableDeclaration<IrSimpleFunctionSymbol>,
    IrOverridableMember,
    IrAttributeContainer {

    abstract override val symbol: IrSimpleFunctionSymbol

    abstract val isTailrec: Boolean
    abstract val isSuspend: Boolean
    abstract val isFakeOverride: Boolean
    abstract val isOperator: Boolean
    abstract val isInfix: Boolean

    abstract var correspondingPropertySymbol: IrPropertySymbol?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSimpleFunction(this, data)
}

val IrFunction.isPropertyAccessor: Boolean
    get() = this is IrSimpleFunction && correspondingPropertySymbol != null
