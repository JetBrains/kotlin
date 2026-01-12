// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

import kotlin.wasm.*

@WasmExport("fInt") fun foo(x: Int) = 1
@WasmExport("fBoolean") fun foo(x: Boolean) = 2
