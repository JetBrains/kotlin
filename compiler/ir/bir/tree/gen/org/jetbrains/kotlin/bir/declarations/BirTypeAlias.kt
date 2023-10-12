/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.symbols.BirTypeAliasSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.transformIfNeeded
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer
import org.jetbrains.kotlin.bir.visitors.BirElementVisitor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.typeAlias]
 */
abstract class BirTypeAlias : BirDeclarationBase(), BirDeclarationWithName,
        BirDeclarationWithVisibility, BirTypeParametersContainer {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: TypeAliasDescriptor

    abstract override val symbol: BirTypeAliasSymbol

    abstract var isActual: Boolean

    abstract var expandedType: BirType

    override fun <R, D> accept(visitor: BirElementVisitor<R, D>, data: D): R =
        visitor.visitTypeAlias(this, data)

    override fun <D> acceptChildren(visitor: BirElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: BirElementTransformer<D>, data: D) {
        typeParameters = typeParameters.transformIfNeeded(transformer, data)
    }
}
