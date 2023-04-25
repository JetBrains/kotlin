/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.field]
 */
abstract class IrField : IrDeclarationBase(), IrPossiblyExternalDeclaration,
        IrDeclarationWithVisibility, IrDeclarationParent, IrMetadataSourceOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: PropertyDescriptor

    abstract override val symbol: IrFieldSymbol

    abstract var type: IrType

    abstract var isFinal: Boolean

    abstract var isStatic: Boolean

    abstract var initializer: IrExpressionBody?

    abstract var correspondingPropertySymbol: IrPropertySymbol?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitField(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        initializer?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        initializer = initializer?.transform(transformer, data)
    }
}
