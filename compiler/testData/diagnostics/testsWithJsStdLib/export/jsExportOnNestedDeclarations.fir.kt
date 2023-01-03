// !OPT_IN: kotlin.js.ExperimentalJsExport

package foo

class C1 {
    @JsExport
    fun f1() {}

    @JsExport
    val p: Int = 10

    @JsExport
    object O
}

fun f2() {
    @JsExport
    fun f3() {}

    @JsExport
    class C2
}
