// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

package foo

<!WRONG_EXPORTED_DECLARATION("suspend function")!>@JsExport
suspend fun suspendFun()<!> { }

@JsExport
class WithSuspendFunctionInside {
    <!WRONG_EXPORTED_DECLARATION("suspend function")!>suspend fun suspendFun()<!> {}
}