/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.symbol

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.validation.checkers.IrSymbolChecker
import org.jetbrains.kotlin.ir.validation.checkers.checkVisibility
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

class IrVisibilityChecker private constructor(private val treatInternalAsPublic: Boolean) : IrSymbolChecker {
    override fun check(
        symbol: IrSymbol,
        container: IrElement,
        context: CheckerContext,
    ) {
        checkVisibility(symbol, container, context, treatInternalAsPublic)
    }

    companion object {
        /**
         * Checks visibilities in "strict" mode: Treats `internal` non-`@PublishedApi` declarations
         * as internal declarations, and `internal @PublishedApi` as public ones.
         *
         * This is the default visibility validation mode.
         */
        val Strict = IrVisibilityChecker(treatInternalAsPublic = false)

        /**
         * Checks visibilities in "relaxed" mode: Treats all `internal` declarations as public declarations.
         *
         * This mode is intended to be used only on the second stage of a KLIB-based compilation
         * and only after all inline functions have been actually inlined.
         * Since there might be access to `internal` non-`@PublishedApi` declarations from other modules,
         * which happens during inlining of `@DoNotInlineOnFirstStage`-marked inline functions.
         */
        val Relaxed = IrVisibilityChecker(treatInternalAsPublic = true)
    }
}