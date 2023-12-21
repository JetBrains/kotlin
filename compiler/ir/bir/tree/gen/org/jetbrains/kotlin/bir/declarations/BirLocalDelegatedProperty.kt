/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.symbols.BirLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.localDelegatedProperty]
 */
abstract class BirLocalDelegatedProperty : BirImplElementBase(), BirElement, BirDeclaration, BirDeclarationWithName, BirSymbolOwner, BirMetadataSourceOwner, BirLocalDelegatedPropertySymbol {
    abstract var type: BirType
    abstract var isVar: Boolean
    abstract var delegate: BirVariable?
    abstract var getter: BirSimpleFunction?
    abstract var setter: BirSimpleFunction?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        delegate?.accept(data, visitor)
        getter?.accept(data, visitor)
        setter?.accept(data, visitor)
    }

    companion object : BirElementClass(BirLocalDelegatedProperty::class.java, 60, true)
}
