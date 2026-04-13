/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isNullable

class WasmAddContinuationToNonLocalSuspendFunctionsLowering(override val context: WasmBackendContext) :
    AddContinuationToNonLocalSuspendFunctionsLowering(context) {

    override fun lowerReturnType(f: IrFunction): IrType =
        when {
            context.wasmCoroutinesStackSwitching -> f.returnType
            f.returnType.isNullable() -> context.irBuiltIns.anyNType
            else -> context.irBuiltIns.anyType
        }
}
