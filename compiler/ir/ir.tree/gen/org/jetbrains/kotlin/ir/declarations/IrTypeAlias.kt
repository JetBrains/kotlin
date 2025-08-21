/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.typeAlias]
 */
abstract class IrTypeAlias : IrDeclarationBase(), IrDeclarationWithName, IrDeclarationWithVisibility, IrTypeParametersContainer, IrMetadataSourceOwner {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: TypeAliasDescriptor

    abstract override val symbol: IrTypeAliasSymbol

    abstract var isActual: Boolean

    abstract var expandedType: IrType

    override fun <R, D> accept(visitor: IrVisitor<R, D>, data: D): R =
        visitor.visitTypeAlias(this, data)

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        typeParameters = typeParameters.transformIfNeeded(transformer, data)
    }
}
