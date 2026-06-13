// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: -AllowInterfaceNestedClassesInJsExport

@JsExport
interface I {
    class <!WRONG_EXPORTED_DECLARATION("nested class inside exported interface")!>Nested<!>
}
