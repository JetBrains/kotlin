// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@JsExport fun foo(x: Int) = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

import kotlin.wasm.*

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@WasmExport fun foo(x: Boolean) = 2<!>
