/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.localDelegatedProperty]
 */
abstract class IrLocalDelegatedProperty : IrDeclarationBase(), IrDeclarationWithName, IrSymbolOwner, IrMetadataSourceOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: VariableDescriptorWithAccessors

    abstract override val symbol: IrLocalDelegatedPropertySymbol

    abstract var type: IrType

    abstract var isVar: Boolean

    abstract var delegate: IrVariable

    abstract var getter: IrSimpleFunction

    abstract var setter: IrSimpleFunction?

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitLocalDelegatedProperty(this, data)

    override fun acceptVoid(visitor: IrVisitorVoid) {
        visitor.visitLocalDelegatedProperty(this)
    }

    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrElement =
        transformer.visitLocalDelegatedProperty(this, data)

    override fun transformVoid(transformer: IrElementTransformerVoid): IrElement =
        transformer.visitLocalDelegatedProperty(this)

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        delegate.accept(visitor, data)
        getter.accept(visitor, data)
        setter?.accept(visitor, data)
    }

    override fun acceptChildrenVoid(visitor: IrVisitorVoid) {
        delegate.acceptVoid(visitor)
        getter.acceptVoid(visitor)
        setter?.acceptVoid(visitor)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        delegate = delegate.transform(transformer, data) as IrVariable
        getter = getter.transform(transformer, data) as IrSimpleFunction
        setter = setter?.transform(transformer, data) as IrSimpleFunction?
    }

    override fun transformChildrenVoid(transformer: IrElementTransformerVoid) {
        delegate = delegate.transformVoid(transformer) as IrVariable
        getter = getter.transformVoid(transformer) as IrSimpleFunction
        setter = setter?.transformVoid(transformer) as IrSimpleFunction?
    }
}
