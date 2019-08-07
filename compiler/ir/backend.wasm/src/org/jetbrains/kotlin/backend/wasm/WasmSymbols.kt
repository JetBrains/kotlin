/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable

class WasmSymbols(
    context: WasmBackendContext,
    private val symbolTable: ReferenceSymbolTable
) : Symbols<WasmBackendContext>(context, symbolTable) {

    override val ThrowNullPointerException
        get() = TODO()
    override val ThrowNoWhenBranchMatchedException
        get() = TODO()
    override val ThrowTypeCastException
        get() = TODO()
    override val ThrowUninitializedPropertyAccessException
        get() = TODO()
    override val defaultConstructorMarker
        get() = TODO()
    override val stringBuilder
        get() = TODO()
    override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
        get() = TODO()
    override val coroutineImpl
        get() = TODO()
    override val coroutineSuspendedGetter
        get() = TODO()
    override val getContinuation
        get() = TODO()
    override val coroutineContextGetter
        get() = TODO()
    override val suspendCoroutineUninterceptedOrReturn
        get() = TODO()
    override val coroutineGetContext
        get() = TODO()
    override val returnIfSuspended
        get() = TODO()
}