// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package foo

import kotlin.wasm.*

<!WASM_EXPORT_CLASH!>@WasmExport fun test() = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package foo

import kotlin.wasm.*

<!WASM_EXPORT_CLASH!>@WasmExport("test") fun bar() = 2<!>
