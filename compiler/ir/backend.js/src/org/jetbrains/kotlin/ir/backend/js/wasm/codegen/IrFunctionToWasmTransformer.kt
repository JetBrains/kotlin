/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.codegen

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrFunctionToWasmTransformer : BaseIrElementToWasmNodeTransformer<JsExpression, WasmStaticContext> {
    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: WasmStaticContext): JsFunction {
        val funcName = if (declaration.dispatchReceiverParameter == null) {
            context.getNameForStaticFunction(declaration)
        } else {
            context.getNameForMemberFunction(declaration)
        }
        return translateFunction(declaration, funcName, context)
    }

    override fun visitConstructor(declaration: IrConstructor, context: WasmStaticContext): JsExpression {
        if (!declaration.isPrimary)
            return JsStringLiteral("!!! Secondary constructor for: ${declaration.parentAsClass.fqNameWhenAvailable}")

        val funcName = context.getNameForConstructor(declaration)
        return translateFunction(declaration, funcName, context)
    }
}