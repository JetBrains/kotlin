/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.isJsNativeGetter
import org.jetbrains.kotlin.ir.backend.js.utils.isJsNativeSetter
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicOperatorExpressionImpl


open class NativeGetterSetterTransformer(val context: JsIrBackendContext) : CallsTransformer {
    override fun transformFunctionAccess(call: IrFunctionAccessExpression, doNotIntrinsify: Boolean): IrExpression {
        val callee = call.symbol.owner

        return when {
            callee.isJsNativeGetter() -> call.transformToIndexedRead()
            callee.isJsNativeSetter() -> call.transformToIndexedWrite()
            // @nativeInvoke is supported separately to simplify processing default arguments,
            // it's harder to support using dynamic operator since the last one doesn't allow arguments with holes.
            // The feature is implemented in `translateCall` in ir/backend/js/transformers/irToJs/jsAstUtils.kt
            // callee.isJsNativeInvoke() -> {}
            else -> call
        }
    }

    protected fun IrFunctionAccessExpression.transformToIndexedRead(): IrExpression {
        val obj = dispatchReceiver ?: extensionReceiver!!
        val propertyName = getValueArgument(0)!!
        return IrDynamicOperatorExpressionImpl(
            startOffset,
            endOffset,
            context.irBuiltIns.anyNType,
            operator = IrDynamicOperator.ARRAY_ACCESS
        ).also {
            it.receiver = obj
            it.arguments.add(propertyName)
        }
    }

    protected fun IrFunctionAccessExpression.transformToIndexedWrite(): IrExpression {
        val value = getValueArgument(1)!!

        return IrDynamicOperatorExpressionImpl(startOffset, endOffset, type, IrDynamicOperator.EQ).also {
            it.receiver = transformToIndexedRead()
            it.arguments.add(value)
        }
    }
}
