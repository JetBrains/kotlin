/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrReturnTarget
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.returnableBlock]
 */
abstract class IrReturnableBlock : IrBlock(), IrSymbolOwner, IrReturnTarget {
    abstract override val symbol: IrReturnableBlockSymbol
}
