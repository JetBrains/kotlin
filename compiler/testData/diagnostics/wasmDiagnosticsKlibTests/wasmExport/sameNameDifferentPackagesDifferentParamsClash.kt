// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

import kotlin.wasm.*

<!WASM_EXPORT_CLASH!>@WasmExport fun foo(x: Int) = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

import kotlin.wasm.*

<!WASM_EXPORT_CLASH!>@WasmExport fun foo(x: Boolean) = 2<!>
