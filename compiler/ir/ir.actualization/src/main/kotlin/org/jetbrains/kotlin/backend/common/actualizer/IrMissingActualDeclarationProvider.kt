/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol

abstract class IrMissingActualDeclarationProvider {
    abstract fun provideSymbolForMissingActual(
        expectSymbol: IrSymbol,
        containingExpectClassSymbol: IrClassSymbol?,
        containingActualClassSymbol: IrClassSymbol?,
    ): IrSymbol?
}