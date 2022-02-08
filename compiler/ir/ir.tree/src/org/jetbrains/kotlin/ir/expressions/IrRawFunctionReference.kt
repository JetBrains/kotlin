/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * Platform-specific low-level reference to function.
 *
 * On JS platform it represents a plain reference to JavaScript function.
 * On JVM platform it represents a MethodHandle constant.
 */
abstract class IrRawFunctionReference : IrDeclarationReference() {
    abstract override val symbol: IrFunctionSymbol

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitRawFunctionReference(this, data)
}
