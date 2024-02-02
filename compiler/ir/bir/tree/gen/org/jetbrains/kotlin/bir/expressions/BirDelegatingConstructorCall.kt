/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.symbols.BirConstructorSymbol

abstract class BirDelegatingConstructorCall(elementClass: BirElementClass<*>) : BirFunctionAccessExpression(elementClass), BirElement {
    abstract override var symbol: BirConstructorSymbol

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        dispatchReceiver?.accept(data, visitor)
        extensionReceiver?.accept(data, visitor)
        valueArguments.acceptChildren(visitor, data)
    }

    companion object : BirElementClass<BirDelegatingConstructorCall>(BirDelegatingConstructorCall::class.java, 18, true) {
        val symbol = BirElementBackReferencesKey<BirDelegatingConstructorCall, _>{ (it as? BirDelegatingConstructorCall)?.symbol?.owner }
    }
}
