/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.symbols.BirFieldSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.field]
 */
abstract class BirField : BirElementBase(), BirDeclaration, BirPossiblyExternalDeclaration,
        BirDeclarationWithVisibility, BirDeclarationParent, BirMetadataSourceOwner, BirFieldSymbol {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: PropertyDescriptor

    abstract var type: BirType

    abstract var isFinal: Boolean

    abstract var isStatic: Boolean

    abstract var initializer: BirExpressionBody?

    abstract var correspondingPropertySymbol: BirPropertySymbol?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        initializer?.accept(data, visitor)
    }

    companion object
}
