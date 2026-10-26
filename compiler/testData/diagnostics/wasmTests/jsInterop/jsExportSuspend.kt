// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// ISSUE: KT-88727
// Exported suspend functions are not reported, but their continuation parameter can't be passed from JS,
// so calling them fails at runtime with "type incompatibility when transforming from/to JS".

// Should be an error
@JsExport
suspend fun suspendInt(): Int = 42

// Should be an error
@JsExport
suspend fun suspendUnit() {}

@JsExport
fun suspendLambdaParameter(block: <!WRONG_JS_INTEROP_TYPE!>suspend () -> Int<!>) {}

@JsExport
fun suspendLambdaReturn(): <!WRONG_JS_INTEROP_TYPE!>suspend () -> Int<!> = { 42 }
