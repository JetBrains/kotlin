// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTICS_MESSAGES
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// LANGUAGE: +JsAllowExportingSuspendFunctions

package foo

<!WRONG_EXPORTED_DECLARATION("suspend function")!>@JsExport
suspend fun suspendFun()<!> { }

@JsExport
class WithSuspendFunctionInside {
    <!WRONG_EXPORTED_DECLARATION("suspend function")!>suspend fun suspendFun()<!> {}
}
