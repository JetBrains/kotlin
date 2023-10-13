/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.symbols.BirFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.function]
 */
interface BirFunction : BirDeclaration, BirPossiblyExternalDeclaration,
        BirDeclarationWithVisibility, BirTypeParametersContainer, BirSymbolOwner,
        BirDeclarationParent, BirReturnTarget, BirMemberWithContainerSource, BirMetadataSourceOwner,
        BirFunctionSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor

    var isInline: Boolean

    var isExpect: Boolean

    var returnType: BirType

    var dispatchReceiverParameter: BirValueParameter?

    var extensionReceiverParameter: BirValueParameter?

    var valueParameters: List<BirValueParameter>

    var contextReceiverParametersCount: Int

    var body: BirBody?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        typeParameters.forEach { it.accept(data, visitor) }
        dispatchReceiverParameter?.accept(data, visitor)
        extensionReceiverParameter?.accept(data, visitor)
        valueParameters.forEach { it.accept(data, visitor) }
        body?.accept(data, visitor)
    }
}
