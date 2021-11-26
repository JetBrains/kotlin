/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.symbols.IrSymbol

// An IR builder with a reference to the JvmBackendContext
class JvmIrBuilder(
    val backendContext: JvmBackendContext,
    val symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET,
) : IrBuilderWithScope(
    IrGeneratorContextBase(backendContext.irBuiltIns),
    Scope(symbol),
    startOffset,
    endOffset
) {
    val irSymbols: JvmSymbols
        get() = backendContext.ir.symbols
}

fun JvmBackendContext.createJvmIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET,
): JvmIrBuilder = JvmIrBuilder(this, symbol, startOffset, endOffset)


fun JvmBackendContext.createJvmIrBuilder(symbol: IrSymbol, source: IrElement): JvmIrBuilder =
    JvmIrBuilder(this, symbol, source.startOffset, source.endOffset)

fun JvmBackendContext.createJvmIrBuilder(scopeWithIr: ScopeWithIr): JvmIrBuilder =
    JvmIrBuilder(this, scopeWithIr.scope.scopeOwnerSymbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)

fun JvmBackendContext.createJvmIrBuilder(scopeWithIr: ScopeWithIr, startOffset: Int, endOffset: Int): JvmIrBuilder =
    JvmIrBuilder(this, scopeWithIr.scope.scopeOwnerSymbol, startOffset, endOffset)

fun JvmBackendContext.createJvmIrBuilder(scopeWithIr: ScopeWithIr, source: IrElement): JvmIrBuilder =
    JvmIrBuilder(this, scopeWithIr.scope.scopeOwnerSymbol, source.startOffset, source.endOffset)
