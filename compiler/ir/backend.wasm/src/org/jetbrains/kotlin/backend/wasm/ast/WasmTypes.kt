/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ast

sealed class WasmValueType(val mnemonic: String)

object WasmI32 : WasmValueType("i32")
object WasmI64 : WasmValueType("i64")
object WasmF32 : WasmValueType("f32")
object WasmF64 : WasmValueType("f64")

object WasmAnyRef : WasmValueType("anyref")