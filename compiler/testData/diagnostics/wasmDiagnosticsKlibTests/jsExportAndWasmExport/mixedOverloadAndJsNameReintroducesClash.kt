// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.kt
@file:Suppress("OPT_IN_USAGE")
package a

import kotlin.wasm.*

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@JsExport @JsName("fInt") fun foo(x: Int) = 1<!>
@WasmExport("fBoolean") fun foo(x: Boolean) = 2

// FILE: B.kt
@file:Suppress("OPT_IN_USAGE")
package b

import kotlin.wasm.*

<!EXPORTING_JS_NAME_WASM_EXPORT_CLASH!>@WasmExport("fInt") fun bar() = 3<!>
