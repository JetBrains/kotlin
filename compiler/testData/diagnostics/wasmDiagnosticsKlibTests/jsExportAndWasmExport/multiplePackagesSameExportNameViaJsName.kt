// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a


<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@JsExport fun foo() = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

import kotlin.wasm.*

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH, WASM_EXPORT_CLASH!>@WasmExport("foo") fun bar() = 2<!>

// FILE: C.kt
@file:Suppress("OPT_IN_USAGE")
package c

import kotlin.wasm.*

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH, WASM_EXPORT_CLASH!>@WasmExport fun foo() = 3<!>
