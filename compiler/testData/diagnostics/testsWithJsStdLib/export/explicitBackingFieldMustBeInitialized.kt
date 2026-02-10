// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ExplicitBackingFields
@JsExport
interface I

value class V(val x: Int) : I

@JsExport
val p: I <!EXPLICIT_FIELD_MUST_BE_INITIALIZED!>field: V<!>
