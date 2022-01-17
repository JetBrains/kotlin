/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrAbstractVisitor

abstract class IrConstructor : IrFunction() {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassConstructorDescriptor
    abstract override val symbol: IrConstructorSymbol

    abstract val isPrimary: Boolean

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)

    override fun <R, D> accept(visitor: IrAbstractVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)
}
