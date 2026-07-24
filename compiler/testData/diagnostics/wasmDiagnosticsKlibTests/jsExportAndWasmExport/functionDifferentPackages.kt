// RUN_PIPELINE_TILL: BACKEND
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package foo

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@JsExport fun test() = 1<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package foo.bar

import kotlin.wasm.*

<!WASM_EXPORT_EXPORTING_JS_NAME_CLASH!>@WasmExport("test") fun bar() = 2<!>
