/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerShallow
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorShallow

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.property]
 */
abstract class IrProperty : IrDeclarationBase(), IrPossiblyExternalDeclaration,
        IrOverridableDeclaration<IrPropertySymbol>, IrMetadataSourceOwner, IrAttributeContainer,
        IrMemberWithContainerSource {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: PropertyDescriptor

    abstract override val symbol: IrPropertySymbol

    abstract var isVar: Boolean

    abstract var isConst: Boolean

    abstract var isLateinit: Boolean

    abstract var isDelegated: Boolean

    abstract var isExpect: Boolean

    abstract override var isFakeOverride: Boolean

    abstract var backingField: IrField?

    abstract var getter: IrSimpleFunction?

    abstract var setter: IrSimpleFunction?

    override fun <R, D> accept(visitor: IrElementVisitorShallow<R, D>, data: D): R =
        visitor.visitProperty(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitorShallow<Unit, D>, data: D) {
        backingField?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformerShallow<D>,
            data: D) {
        backingField = backingField?.transform(transformer, data) as IrField?
        getter = getter?.transform(transformer, data) as IrSimpleFunction?
        setter = setter?.transform(transformer, data) as IrSimpleFunction?
    }
}
