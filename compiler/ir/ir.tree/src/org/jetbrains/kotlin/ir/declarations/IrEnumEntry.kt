/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrAbstractVisitor

abstract class IrEnumEntry : IrDeclarationBase(), IrDeclarationWithName {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassDescriptor
    abstract override val symbol: IrEnumEntrySymbol

    abstract var correspondingClass: IrClass?
    abstract var initializerExpression: IrExpressionBody?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitEnumEntry(this, data)

    override fun <R, D> accept(visitor: IrAbstractVisitor<R, D>, data: D): R =
        visitor.visitEnumEntry(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializerExpression?.accept(visitor, data)
        correspondingClass?.accept(visitor, data)
    }

    override fun <D> acceptChildren(visitor: IrAbstractVisitor<Unit, D>, data: D) {
        initializerExpression?.accept(visitor, data)
        correspondingClass?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        initializerExpression = initializerExpression?.transform(transformer, data)
        correspondingClass = correspondingClass?.transform(transformer, data) as? IrClass
    }
}
