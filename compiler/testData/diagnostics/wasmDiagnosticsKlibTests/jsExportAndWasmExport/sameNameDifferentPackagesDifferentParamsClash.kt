// RUN_PIPELINE_TILL: BACKEND
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@JsExport fun foo(x: Int) = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

import kotlin.wasm.*

<!WASM_EXPORT_EXPORTING_JS_NAME_CLASH!>@WasmExport fun foo(x: Boolean) = 2<!>
