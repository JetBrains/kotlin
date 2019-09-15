/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.hasExcludedFromCodegenAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasSkipRTTIAnnotation
import org.jetbrains.kotlin.ir.backend.js.lower.BridgesConstruction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name

class WasmBridgesConstruction(context: CommonBackendContext) : BridgesConstruction<WasmSignature>(context) {
    override fun getFunctionSignature(function: IrSimpleFunction): WasmSignature =
        function.wasmSignature()

    override val castDispatchReceiver: Boolean = true

    override fun shouldSkipFunction(function: IrSimpleFunction): Boolean {
        return function.hasExcludedFromCodegenAnnotation() || function.parentAsClass.hasSkipRTTIAnnotation()
    }
}

data class WasmSignature(
    val name: Name,
    val dispatchReceiverType: IrType?,
    val extensionReceiverType: IrType?,
    val valueParametersType: List<IrType>,
    val returnType: IrType
) {
    override fun toString(): String {
        val dr = dispatchReceiverType?.let { "(dr: ${it.render()}) " } ?: ""
        val er = extensionReceiverType?.let { "(er: ${it.render()}) " } ?: ""
        val parameters = valueParametersType.joinToString(", ") { it.render() }
        return "[$dr$er$name($parameters) -> ${returnType.render()}]"
    }
}

fun IrSimpleFunction.wasmSignature(): WasmSignature =
    WasmSignature(
        name,
        dispatchReceiverParameter?.type,
        extensionReceiverParameter?.type,
        valueParameters.map { it.type },
        returnType
    )

