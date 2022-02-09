/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrElement {
    val startOffset: Int

    val endOffset: Int

    fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R

    fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrElement

    fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D)

    fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D)
}
