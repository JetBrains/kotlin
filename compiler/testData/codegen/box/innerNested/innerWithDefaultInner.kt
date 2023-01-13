// IGNORE_BACKEND_K2: JS_IR, NATIVE

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