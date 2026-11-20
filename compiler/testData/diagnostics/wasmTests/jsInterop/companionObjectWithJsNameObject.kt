// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87862
// RENDER_DIAGNOSTICS_FULL_TEXT

@JsName("Object")
external interface JsNameObjectInterface {
    companion <!COMPANION_OBJECT_IN_EXTERNAL_INTERFACE!>object<!>
}
