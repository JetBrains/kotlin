/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrPropertyReference(typeArgumentsCount: Int) : IrCallableReference<IrPropertySymbol>(typeArgumentsCount) {
    abstract val field: IrFieldSymbol?
    abstract val getter: IrSimpleFunctionSymbol?
    abstract val setter: IrSimpleFunctionSymbol?

    override val valueArgumentsCount: Int
        get() = 0

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitPropertyReference(this, data)
}
