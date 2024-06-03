/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementBackReferencesKey
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.symbols.BirFunctionSymbol

abstract class BirFunctionAccessExpression() : BirMemberAccessExpression<BirFunctionSymbol>() {
    abstract var contextReceiversCount: Int

    companion object : BirElementClass<BirFunctionAccessExpression>(BirFunctionAccessExpression::class.java, 49, false) {
        val symbol = BirElementBackReferencesKey<BirFunctionAccessExpression, _>{ (it as? BirFunctionAccessExpression)?.symbol?.owner }
    }
}
