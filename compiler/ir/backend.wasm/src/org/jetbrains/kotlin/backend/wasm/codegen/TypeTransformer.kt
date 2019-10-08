/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.render


fun WasmCodegenContext.transformType(irType: IrType): WasmValueType =
    when {
        irType.isBoolean() -> WasmI32
        irType.isByte() -> WasmI32
        irType.isShort() -> WasmI32
        irType.isInt() -> WasmI32
        irType.isLong() -> WasmI64
        irType.isChar() -> WasmI32
        irType.isFloat() -> WasmF32
        irType.isDouble() -> WasmF64
        irType.isString() -> WasmAnyRef
        irType.isAny() || irType.isNullableAny() -> WasmAnyRef
        else ->
            TODO("Unsupported type: ${irType.render()}")
    }