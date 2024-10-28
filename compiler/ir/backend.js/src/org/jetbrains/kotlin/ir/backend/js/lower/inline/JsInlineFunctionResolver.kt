/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolverReplacingCoroutineIntrinsics
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

internal class JsInlineFunctionResolver(
    context: JsIrBackendContext,
    inlineMode: InlineMode,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<JsIrBackendContext>(context, inlineMode) {
    override val allowExternalInlining: Boolean = true
}
