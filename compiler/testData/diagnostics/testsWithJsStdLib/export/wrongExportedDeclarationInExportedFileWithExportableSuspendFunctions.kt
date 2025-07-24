// OPT_IN: kotlin.js.ExperimentalJsExport
// RENDER_DIAGNOSTICS_MESSAGES
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED
// LANGUAGE: +JsAllowExportingSuspendFunctions
@file:JsExport
package foo

<!WRONG_EXPORTED_DECLARATION("suspend function")!>suspend fun suspendFun()<!> { }

class WithSuspendFunctionInside {
    <!WRONG_EXPORTED_DECLARATION("suspend function")!>suspend fun suspendFun()<!> {}
}
