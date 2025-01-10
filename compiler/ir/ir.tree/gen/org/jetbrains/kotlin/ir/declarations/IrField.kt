/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.field]
 */
abstract class IrField : IrDeclarationBase(), IrPossiblyExternalDeclaration, IrDeclarationWithVisibility, IrDeclarationParent, IrMetadataSourceOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: PropertyDescriptor

    abstract override val symbol: IrFieldSymbol

    abstract var type: IrType

    abstract var isFinal: Boolean

    abstract var isStatic: Boolean

    abstract var initializer: IrExpressionBody?

    abstract var correspondingPropertySymbol: IrPropertySymbol?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitField(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitField(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrElement =
        transformer.visitField(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrElement =
        transformer.visitField(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        initializer?.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        initializer = initializer?.transform(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        initializer = initializer?.transformVoid(transformer)
    }
}
