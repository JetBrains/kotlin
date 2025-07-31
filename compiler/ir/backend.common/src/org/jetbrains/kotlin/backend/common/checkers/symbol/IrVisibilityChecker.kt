/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.symbol

import org.jetbrains.kotlin.backend.common.checkers.checkVisibility
import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.symbols.IrSymbol

object IrVisibilityChecker : IrSymbolChecker {
    override fun check(
        symbol: IrSymbol,
        container: IrElement,
        context: CheckerContext,
    ) {
        checkVisibility(symbol, container, context)
    }
}