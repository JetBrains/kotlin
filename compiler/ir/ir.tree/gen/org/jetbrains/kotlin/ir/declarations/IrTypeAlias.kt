/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformer
import org.jetbrains.kotlin.ir.visitors.IrLeafTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitorVoid

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.typeAlias]
 */
abstract class IrTypeAlias : IrDeclarationBase(), IrDeclarationWithName, IrDeclarationWithVisibility, IrTypeParametersContainer, IrMetadataSourceOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: TypeAliasDescriptor

    abstract override val symbol: IrTypeAliasSymbol

    abstract var isActual: Boolean

    abstract var expandedType: IrType

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitTypeAlias(this, data)

    override fun acceptVoid(visitor: IrLeafVisitorVoid) {
        visitor.visitTypeAlias(this)
    }

    override fun <D> transform(transformer: IrLeafTransformer<D>, data: D): IrElement =
        transformer.visitTypeAlias(this, data)

    override fun transformVoid(transformer: IrLeafTransformerVoid): IrElement =
        transformer.visitTypeAlias(this)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun acceptChildrenVoid(visitor: IrLeafVisitorVoid) {
        typeParameters.forEach { it.acceptVoid(visitor) }
    }

    override fun <D> transformChildren(transformer: IrLeafTransformer<D>, data: D) {
        typeParameters = typeParameters.transformIfNeeded(transformer, data)
    }

    override fun transformChildrenVoid(transformer: IrLeafTransformerVoid) {
        typeParameters = typeParameters.transformIfNeeded(transformer)
    }
}
