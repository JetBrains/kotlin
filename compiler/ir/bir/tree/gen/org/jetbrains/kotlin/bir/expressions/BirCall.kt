/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol

abstract class BirCall(elementClass: BirElementClass<*>) : BirFunctionAccessExpression(elementClass), BirElement {
    abstract override var symbol: BirSimpleFunctionSymbol
    abstract var superQualifierSymbol: BirClassSymbol?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        dispatchReceiver?.accept(data, visitor)
        extensionReceiver?.accept(data, visitor)
        valueArguments.acceptChildren(visitor, data)
    }

    companion object : BirElementClass<BirCall>(BirCall::class.java, 6, true) {
        val symbol = BirElementBackReferencesKey<BirCall, _>{ (it as? BirCall)?.symbol?.owner }
    }
}
