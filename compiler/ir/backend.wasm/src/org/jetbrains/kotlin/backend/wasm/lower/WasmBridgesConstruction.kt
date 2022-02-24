/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.BridgesConstruction
import org.jetbrains.kotlin.ir.backend.js.utils.eraseGenerics
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isOverridableOrOverrides
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name

class WasmBridgesConstruction(context: JsCommonBackendContext) : BridgesConstruction<JsCommonBackendContext>(context) {
    override fun getFunctionSignature(function: IrSimpleFunction): WasmSignature =
        function.wasmSignature(context.irBuiltIns)

    // Dispatch receiver type must be casted when types are different.
    override val shouldCastDispatchReceiver: Boolean = true
    override fun getBridgeOrigin(bridge: IrSimpleFunction): IrDeclarationOrigin =
        IrDeclarationOrigin.BRIDGE
}

data class WasmSignature(
    val name: Name,
    val extensionReceiverType: IrType?,
    val valueParametersType: List<IrType>,
    val returnType: IrType,
    // Needed for bridges to final non-override methods
    // that indirectly implement interfaces. For example:
    //    interface I { fun foo() }
    //    class C1 { fun foo() {} }
    //    class C2 : C1(), I
    val isVirtual: Boolean,
) {
    override fun toString(): String {
        val er = extensionReceiverType?.let { "(er: ${it.render()}) " } ?: ""
        val parameters = valueParametersType.joinToString(", ") { it.render() }
        val nonVirtual = if (!isVirtual) "(non-virtual) " else ""
        return "[$nonVirtual$er$name($parameters) -> ${returnType.render()}]"
    }
}

fun IrSimpleFunction.wasmSignature(irBuiltIns: IrBuiltIns): WasmSignature =
    WasmSignature(
        name,
        extensionReceiverParameter?.type?.eraseGenerics(irBuiltIns),
        valueParameters.map { it.type.eraseGenerics(irBuiltIns) },
        returnType.eraseGenerics(irBuiltIns),
        isOverridableOrOverrides
    )