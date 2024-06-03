/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementBackReferencesKey
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.symbols.BirLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.symbols.BirVariableSymbol
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

abstract class BirLocalDelegatedPropertyReference() : BirCallableReference<BirLocalDelegatedPropertySymbol>() {
    abstract var delegate: BirVariableSymbol

    abstract var getter: BirSimpleFunctionSymbol

    abstract var setter: BirSimpleFunctionSymbol?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        dispatchReceiver?.accept(data, visitor)
        extensionReceiver?.accept(data, visitor)
        valueArguments.acceptChildren(visitor, data)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirLocalDelegatedPropertyReference

    companion object : BirElementClass<BirLocalDelegatedPropertyReference>(BirLocalDelegatedPropertyReference::class.java, 62, true) {
        val symbol = BirElementBackReferencesKey<BirLocalDelegatedPropertyReference, _>{ (it as? BirLocalDelegatedPropertyReference)?.symbol?.owner }
    }
}
