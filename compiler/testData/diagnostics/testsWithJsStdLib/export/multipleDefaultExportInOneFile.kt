// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport

<!MULTIPLE_JS_EXPORT_DEFAULT_IN_ONE_FILE!>@JsExport.Default
fun defaultFun() = "OK"<!>

<!MULTIPLE_JS_EXPORT_DEFAULT_IN_ONE_FILE!>@JsExport.Default
class DefaultClass<!>

// We do not garantee MULTIPLE_JS_EXPORT_DEFAULT_IN_ONE_FILE reporting in such cases.
@JsExport
@JsName("default")
object DefaultObject
