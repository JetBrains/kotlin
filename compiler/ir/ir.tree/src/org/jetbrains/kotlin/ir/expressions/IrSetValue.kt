/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrValueSymbol

abstract class IrSetValue : IrValueAccessExpression() {
    abstract override val symbol: IrValueSymbol
    abstract var value: IrExpression
}
