/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.irCall

class ExceptionHelperCallsTransformer(context: JsIrBackendContext) : CallsTransformer {
    private val helperMapping = mapOf(
        context.irBuiltIns.checkNotNullSymbol to context.symbols.jsEnsureNonNull,
        context.irBuiltIns.throwCceSymbol to context.symbols.throwTypeCastException,
        context.irBuiltIns.throwIseSymbol to context.symbols.throwISE,
        context.irBuiltIns.illegalArgumentExceptionSymbol to context.symbols.throwIAE,
        context.irBuiltIns.noWhenBranchMatchedExceptionSymbol to context.symbols.noWhenBranchMatchedException,
        context.irBuiltIns.linkageErrorSymbol to context.symbols.linkageErrorSymbol,
    )

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean) =
        helperMapping[call.symbol]?.let { irCall(call, it) } ?: call
}
