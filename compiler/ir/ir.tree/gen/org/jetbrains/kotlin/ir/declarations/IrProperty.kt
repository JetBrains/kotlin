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
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.property]
 */
abstract class IrProperty : IrDeclarationBase(), IrPossiblyExternalDeclaration, IrOverridableDeclaration<IrPropertySymbol>, IrMetadataSourceOwner, IrMemberWithContainerSource {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: PropertyDescriptor

    abstract override val symbol: IrPropertySymbol

    abstract override var overriddenSymbols: List<IrPropertySymbol>

    abstract var isVar: Boolean

    abstract var isConst: Boolean

    abstract var isLateinit: Boolean

    abstract var isDelegated: Boolean

    abstract var isExpect: Boolean

    abstract var backingField: IrField?

    abstract var getter: IrSimpleFunction?

    abstract var setter: IrSimpleFunction?

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitProperty(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrElement =
        transformer.visitProperty(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrElement =
        transformer.visitProperty(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        backingField?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        backingField?.acceptVoid(visitor)
        getter?.acceptVoid(visitor)
        setter?.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        backingField = backingField?.transform(transformer, data) as IrField?
        getter = getter?.transform(transformer, data) as IrSimpleFunction?
        setter = setter?.transform(transformer, data) as IrSimpleFunction?
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        backingField = backingField?.transformVoid(transformer) as IrField?
        getter = getter?.transformVoid(transformer) as IrSimpleFunction?
        setter = setter?.transformVoid(transformer) as IrSimpleFunction?
    }
}
