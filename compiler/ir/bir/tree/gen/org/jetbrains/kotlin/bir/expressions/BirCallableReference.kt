/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.BirElementBackReferencesKey
import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.symbols.BirSymbol

abstract class BirCallableReference<S : BirSymbol>() : BirMemberAccessExpression<S>() {
    abstract override var symbol: S

    companion object : BirElementClass<BirCallableReference<*>>(BirCallableReference::class.java, 10, false) {
        val symbol = BirElementBackReferencesKey<BirCallableReference<*>, _>{ (it as? BirCallableReference<*>)?.symbol?.owner }
    }
}
