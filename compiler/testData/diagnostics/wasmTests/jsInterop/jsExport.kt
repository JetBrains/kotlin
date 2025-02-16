// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ContextParameters

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

<!WRONG_ANNOTATION_TARGET!>@JsExport<!>
class C2

<!WRONG_ANNOTATION_TARGET!>@JsExport<!>
var p2: Int = 1

@JsExport
fun fooUnsigned1(): UInt = 42u

@JsExport
fun fooUnsigned2(): UByte = 42u

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(x: <!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>)<!>
@JsExport
fun fooWithContext() {
}
