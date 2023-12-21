/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.property]
 */
interface BirProperty : BirElement, BirDeclaration, BirPossiblyExternalDeclaration, BirOverridableDeclaration<BirPropertySymbol>, BirMetadataSourceOwner, BirAttributeContainer, BirMemberWithContainerSource, BirPropertySymbol {
    var isVar: Boolean
    var isConst: Boolean
    var isLateinit: Boolean
    var isDelegated: Boolean
    var isExpect: Boolean
    override var isFakeOverride: Boolean
    var backingField: BirField?
    var getter: BirSimpleFunction?
    var setter: BirSimpleFunction?
    override var overriddenSymbols: List<BirPropertySymbol>

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        backingField?.accept(data, visitor)
        getter?.accept(data, visitor)
        setter?.accept(data, visitor)
    }

    companion object : BirElementClass(BirProperty::class.java, 43, true)
}
