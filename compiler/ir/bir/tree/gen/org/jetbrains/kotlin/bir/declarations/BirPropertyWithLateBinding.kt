/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.propertyWithLateBinding]
 */
abstract class BirPropertyWithLateBinding : BirProperty() {
    abstract val isBound: Boolean

    abstract fun acquireSymbol(symbol: BirPropertySymbol): BirPropertyWithLateBinding
}
