/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.enumEntry]
 */
abstract class IrEnumEntry : IrDeclarationBase(), IrDeclarationWithName {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ClassDescriptor

    abstract override val symbol: IrEnumEntrySymbol

    abstract var initializerExpression: IrExpressionBody?

    abstract var correspondingClass: IrClass?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitEnumEntry(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitEnumEntry(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrElement =
        transformer.visitEnumEntry(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrElement =
        transformer.visitEnumEntry(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        initializerExpression?.accept(visitor, data)
        correspondingClass?.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        initializerExpression?.acceptVoid(visitor)
        correspondingClass?.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        initializerExpression = initializerExpression?.transform(transformer, data)
        correspondingClass = correspondingClass?.transform(transformer, data) as IrClass?
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        initializerExpression = initializerExpression?.transformVoid(transformer)
        correspondingClass = correspondingClass?.transformVoid(transformer) as IrClass?
    }
}
