// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport, kotlin.js.ExperimentalJsNoRuntime
// RENDER_DIAGNOSTIC_ARGUMENTS

// FILE: main.kt
package foo

import kotlin.js.JsNoRuntime

interface NotExported {
    fun hidden(): String
}

@JsExport
@JsNoRuntime
interface PublicNoRuntime : <!EXPOSED_NOT_EXPORTED_SUPER_INTERFACE_WARNING("NotExported")!>NotExported<!> {
    fun visible(): String
}
