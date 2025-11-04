/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.inline.FunctionInlining
import org.jetbrains.kotlin.ir.inline.InlineMode

internal class JsPrivateFunctionInlining(context: JsIrBackendContext) : FunctionInlining(
    context,
    JsInlineFunctionResolver(context, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS),
)

internal class JsAllFunctionInlining(context: JsIrBackendContext) : FunctionInlining(
    context,
    JsInlineFunctionResolver(context, inlineMode = InlineMode.ALL_INLINE_FUNCTIONS),
)