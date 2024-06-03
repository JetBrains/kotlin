/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirChildElementList
import org.jetbrains.kotlin.bir.BirElementBackReferencesKey
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

abstract class BirMemberAccessExpression<S : BirSymbol>() : BirDeclarationReference() {
    abstract var dispatchReceiver: BirExpression?

    abstract var extensionReceiver: BirExpression?

    abstract override val symbol: S

    abstract var origin: IrStatementOrigin?

    abstract val valueArguments: BirChildElementList<BirExpression?>

    abstract var typeArguments: List<BirType?>

    companion object : BirElementClass<BirMemberAccessExpression<*>>(BirMemberAccessExpression::class.java, 64, false) {
        val symbol = BirElementBackReferencesKey<BirMemberAccessExpression<*>, _>{ (it as? BirMemberAccessExpression<*>)?.symbol?.owner }
    }
}
