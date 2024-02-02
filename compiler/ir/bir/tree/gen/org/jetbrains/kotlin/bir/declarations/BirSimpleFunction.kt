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
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

interface BirSimpleFunction : BirFunction, BirOverridableDeclaration<BirSimpleFunctionSymbol>, BirAttributeContainer {
    override val symbol: BirSimpleFunctionSymbol

    override var overriddenSymbols: List<BirSimpleFunctionSymbol>

    var isTailrec: Boolean

    var isSuspend: Boolean

    override var isFakeOverride: Boolean

    var isOperator: Boolean

    var isInfix: Boolean

    var correspondingPropertySymbol: BirPropertySymbol?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        typeParameters.acceptChildren(visitor, data)
        dispatchReceiverParameter?.accept(data, visitor)
        extensionReceiverParameter?.accept(data, visitor)
        valueParameters.acceptChildren(visitor, data)
        body?.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirSimpleFunction

    companion object : BirElementClass<BirSimpleFunction>(BirSimpleFunction::class.java, 83, true)
}
