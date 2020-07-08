/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicOperatorExpressionImpl
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName


// TODO use nativeX annotations on kotlin.js.Json instead
class JsonIntrinsics(context: JsIrBackendContext) : NativeGetterSetterTransformer(context) {

    override fun transformFunctionAccess(call: IrFunctionAccessExpression, skip: Boolean): IrExpression {
        return when (call.symbol.owner.fqNameWhenAvailable) {
            FqName("kotlin.js.Json.get") -> call.transformToIndexedRead()
            FqName("kotlin.js.Json.set") -> call.transformToIndexedWrite()
            else -> call
        }
    }
}
