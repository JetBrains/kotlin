/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrReturnTarget
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol

abstract class IrReturnableBlock : IrBlock(), IrSymbolOwner, IrReturnTarget {
    abstract override val symbol: IrReturnableBlockSymbol

    abstract val inlineFunctionSymbol: IrFunctionSymbol?
}
