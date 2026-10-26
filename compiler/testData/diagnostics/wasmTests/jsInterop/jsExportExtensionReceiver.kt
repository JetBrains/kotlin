// RUN_PIPELINE_TILL: CODEGEN
// OPT_IN: kotlin.js.ExperimentalJsExport
// ISSUE: KT-90295
// The receiver type of an exported extension function is not checked, so non-external receiver types
// are accepted and fail at runtime with "type incompatibility when transforming from/to JS".

external interface ExternalInterface

class NonExternalClass

@JsExport
fun Int.intReceiver(): Int = this

@JsExport
fun String.stringReceiver(): String = this

@JsExport
fun ExternalInterface.externalReceiver(): Int = 0

// Should be WRONG_JS_INTEROP_TYPE
@JsExport
fun NonExternalClass.nonExternalReceiver(): Int = 0

// Should be WRONG_JS_INTEROP_TYPE
@JsExport
fun Any.anyReceiver(): Int = 0

// Should be WRONG_JS_INTEROP_TYPE
@JsExport
fun List<Int>.listReceiver(): Int = 0
