/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.BirImplElementBase
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

abstract class BirLocalDelegatedProperty() : BirImplElementBase(), BirDeclarationBase, BirDeclarationWithName, BirSymbolOwner, BirMetadataSourceOwner {
    abstract override val symbol: BirLocalDelegatedPropertySymbol

    abstract var type: BirType

    abstract var isVar: Boolean

    abstract var delegate: BirVariable

    abstract var getter: BirSimpleFunction

    abstract var setter: BirSimpleFunction?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        delegate.accept(data, visitor)
        getter.accept(data, visitor)
        setter?.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirLocalDelegatedProperty

    companion object : BirElementClass<BirLocalDelegatedProperty>(BirLocalDelegatedProperty::class.java, 61, true)
}
