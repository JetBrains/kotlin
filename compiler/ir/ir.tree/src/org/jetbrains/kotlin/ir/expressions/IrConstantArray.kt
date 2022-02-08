/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrConstantArray : IrConstantValue() {
    abstract val elements: MutableList<IrConstantValue>

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstantArray(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        elements.forEach { value -> value.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        elements.transformInPlace { value -> value.transform(transformer, data) }
    }
}
