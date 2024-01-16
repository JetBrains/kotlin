/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmSignature
import org.jetbrains.kotlin.backend.wasm.ir2wasm.wasmSignature
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.BridgesConstruction
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal

class WasmBridgesConstruction(context: JsCommonBackendContext) : BridgesConstruction<JsCommonBackendContext>(context) {
    override fun getFunctionSignature(function: IrSimpleFunction): WasmSignature =
        function.wasmSignature(context.irBuiltIns)

    // Dispatch receiver type must be casted when types are different.
    override val shouldCastDispatchReceiver: Boolean = true
    override fun getBridgeOrigin(bridge: IrSimpleFunction): IrDeclarationOrigin =
        IrDeclarationOrigin.BRIDGE

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration.isEffectivelyExternal()) return null
        return super.transformFlat(declaration)
    }
}