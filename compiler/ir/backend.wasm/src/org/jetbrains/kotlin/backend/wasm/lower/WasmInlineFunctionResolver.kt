/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolverReplacingCoroutineIntrinsics
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

class WasmInlineFunctionResolver(
    context: WasmBackendContext,
    inlineMode: InlineMode,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<WasmBackendContext>(context, inlineMode)