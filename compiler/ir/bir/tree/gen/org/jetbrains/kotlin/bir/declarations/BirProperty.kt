/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

interface BirProperty : BirDeclarationBase, BirDeclarationParent, BirPossiblyExternalDeclaration, BirOverridableDeclaration<BirPropertySymbol>, BirMetadataSourceOwner, BirAttributeContainer, BirMemberWithContainerSource {
    override val symbol: BirPropertySymbol

    override var overriddenSymbols: List<BirPropertySymbol>

    var isVar: Boolean

    var isConst: Boolean

    var isLateinit: Boolean

    var isDelegated: Boolean

    var isExpect: Boolean

    override var isFakeOverride: Boolean

    var backingField: BirField?

    var getter: BirSimpleFunction?

    var setter: BirSimpleFunction?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        backingField?.accept(data, visitor)
        getter?.accept(data, visitor)
        setter?.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirProperty

    companion object : BirElementClass<BirProperty>(BirProperty::class.java, 73, true)
}
