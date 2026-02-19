// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@JsExport @JsName("f") fun foo() = 0<!>

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

import kotlin.wasm.*

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@WasmExport("f") fun bar(x: Int) = x<!>
