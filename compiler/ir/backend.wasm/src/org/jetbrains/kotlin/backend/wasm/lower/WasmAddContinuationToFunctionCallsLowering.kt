/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.AddContinuationToFunctionCallsLowering
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.types.IrType

@PhasePrerequisites(AddContinuationToNonLocalSuspendFunctionsLowering::class)
class WasmAddContinuationToFunctionCallsLowering(
    override val context: WasmBackendContext
) : AddContinuationToFunctionCallsLowering(context) {
    override fun getReturnType(expression: IrCall, newFun: IrSimpleFunction): IrType =
        if (context.wasmCoroutinesStackSwitching) {
            expression.type
        } else {
            super.getReturnType(expression, newFun)
        }
}
