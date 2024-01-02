// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, WASM
// K2 issue: KT-64801
// KT-40686


class Outer(val o: String, val oo: String) {
    inner class InnerArg(val i: String) {
        val result: String get() = o + i
    }

    inner class InnerParam(val i: InnerArg = InnerArg("B")) {
        fun foo() = i.result + oo
    }
}


fun box(): String {
    val o = Outer("A", "C")
    val i = o.InnerParam()

    val rr = i.foo()
    if (rr != "ABC") return "FAIL: $rr"

    return "OK"
}
