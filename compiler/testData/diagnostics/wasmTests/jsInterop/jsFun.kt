// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// FIR_IDENTICAL
@JsFun("() => {}")
external fun topLevelExternalFun(): Unit

external class ExternalClass {
    <!WRONG_JS_FUN_TARGET!>@JsFun("() => {}")<!>
    fun memberFun(): Unit
}

<!WRONG_JS_FUN_TARGET!>@JsFun("() => {}")<!>
fun topLevelNonExternalFun(): Unit {}

class NonExternalClass {
    <!WRONG_JS_FUN_TARGET!>@JsFun("() => {}")<!>
    fun memberFun(): Unit {}
}
