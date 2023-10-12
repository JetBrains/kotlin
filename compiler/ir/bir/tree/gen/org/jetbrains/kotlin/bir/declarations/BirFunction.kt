/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.symbols.BirFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.transformIfNeeded
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer
import org.jetbrains.kotlin.bir.visitors.BirElementVisitor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.function]
 */
abstract class BirFunction : BirDeclarationBase(), BirPossiblyExternalDeclaration,
        BirDeclarationWithVisibility, BirTypeParametersContainer, BirSymbolOwner,
        BirDeclarationParent, BirReturnTarget, BirMemberWithContainerSource, BirMetadataSourceOwner
        {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: FunctionDescriptor

    abstract override val symbol: BirFunctionSymbol

    abstract var isInline: Boolean

    abstract var isExpect: Boolean

    abstract var returnType: BirType

    abstract var dispatchReceiverParameter: BirValueParameter?

    abstract var extensionReceiverParameter: BirValueParameter?

    abstract var valueParameters: List<BirValueParameter>

    abstract var contextReceiverParametersCount: Int

    abstract var body: BirBody?

    override fun <D> acceptChildren(visitor: BirElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        dispatchReceiverParameter?.accept(visitor, data)
        extensionReceiverParameter?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: BirElementTransformer<D>, data: D) {
        typeParameters = typeParameters.transformIfNeeded(transformer, data)
        dispatchReceiverParameter = dispatchReceiverParameter?.transform(transformer, data)
        extensionReceiverParameter = extensionReceiverParameter?.transform(transformer, data)
        valueParameters = valueParameters.transformIfNeeded(transformer, data)
        body = body?.transform(transformer, data)
    }
}
