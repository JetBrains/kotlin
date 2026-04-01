// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTIC_ARGUMENTS

typealias A = Int

@Suppress("UNUSED_PARAMETER")
@JsExport
fun test(x: A) {}
