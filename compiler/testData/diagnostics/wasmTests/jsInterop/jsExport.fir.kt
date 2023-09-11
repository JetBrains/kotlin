// !OPT_IN: kotlin.js.ExperimentalJsExport

@JsExport
fun foo1() {
}

class C {
    @JsExport
    fun memberFunction() {
    }
}

fun foo2() {
    @JsExport
    fun localFun() {
    }
}

val p1 = (@JsExport fun () {})

<!WRONG_ANNOTATION_TARGET!>@JsExport<!>
class C2

<!WRONG_ANNOTATION_TARGET!>@JsExport<!>
var p2: Int = 1
