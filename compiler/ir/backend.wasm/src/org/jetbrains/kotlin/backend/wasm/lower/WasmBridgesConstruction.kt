/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.erasedUpperBound
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.BridgesConstruction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name

class WasmBridgesConstruction(context: JsCommonBackendContext) : BridgesConstruction(context) {
    override fun getFunctionSignature(function: IrSimpleFunction): WasmSignature =
        function.wasmSignature(context.irBuiltIns)

    // Dispatch receiver type must be casted when types are different.
    override val shouldCastDispatchReceiver: Boolean = true
}

data class WasmSignature(
    val name: Name,
    val extensionReceiverType: IrType?,
    val valueParametersType: List<IrType>,
    val returnType: IrType
) {
    override fun toString(): String {
        val er = extensionReceiverType?.let { "(er: ${it.render()}) " } ?: ""
        val parameters = valueParametersType.joinToString(", ") { it.render() }
        return "[$er$name($parameters) -> ${returnType.render()}]"
    }
}

fun IrSimpleFunction.wasmSignature(irBuiltIns: IrBuiltIns): WasmSignature =
    WasmSignature(
        name,
        extensionReceiverParameter?.type?.eraseGenerics(irBuiltIns),
        valueParameters.map { it.type.eraseGenerics(irBuiltIns) },
        returnType.eraseGenerics(irBuiltIns)
    )

private fun IrType.eraseGenerics(irBuiltIns: IrBuiltIns): IrType {
    val defaultType = this.erasedUpperBound?.defaultType ?: irBuiltIns.anyType
    if (!this.isNullable()) return defaultType
    return defaultType.makeNullable()
}

