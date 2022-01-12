/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrBlockBody : IrBody(), IrStatementContainer {
    abstract val factory: IrFactory

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitBlockBody(this, data)
}
