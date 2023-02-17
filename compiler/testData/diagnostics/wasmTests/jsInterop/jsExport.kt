// !OPT_IN: kotlin.js.ExperimentalJsExport

@JsExport
fun foo1() {
}

class C {
    <!NESTED_JS_EXPORT!>@JsExport<!>
    fun memberFunction() {
    }
}

fun foo2() {
    <!NESTED_JS_EXPORT!>@JsExport<!>
    fun localFun() {
    }
}

val p1 = (<!NESTED_JS_EXPORT!>@JsExport<!> fun () {})