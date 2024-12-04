/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.ClassReferenceLowering
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.IrType

class WasmClassReferenceLowering(context: WasmBackendContext) : ClassReferenceLowering(context) {
    override fun callGetKClass(
        returnType: IrType,
        typeArgument: IrType
    ): IrCall {
        val primitiveKClass =
            getFinalPrimitiveKClass(returnType, typeArgument) ?: getOpenPrimitiveKClass(returnType, typeArgument)

        if (primitiveKClass != null)
            return primitiveKClass

        return JsIrBuilder.buildCall(reflectionSymbols.getKClass, returnType).also {
            it.putTypeArgument(0, typeArgument)
        }
    }
}