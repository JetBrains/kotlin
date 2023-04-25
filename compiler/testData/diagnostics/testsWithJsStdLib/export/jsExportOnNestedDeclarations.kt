// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport

package foo

class C1 {
    <!NESTED_JS_EXPORT!>@JsExport<!>
    fun f1() {}

    <!NESTED_JS_EXPORT!>@JsExport<!>
    val p: Int = 10

    <!NESTED_JS_EXPORT!>@JsExport<!>
    object O
}

fun f2() {
    <!NESTED_JS_EXPORT!>@JsExport<!>
    fun f3() {}

    <!NESTED_JS_EXPORT!>@JsExport<!>
    class C2
}
