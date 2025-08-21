// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JVM_IR, JVM_IR_SERIALIZE
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// ^KT-76439
// FILE: 1.kt

var s = ""

open class A<T> {
    inline fun test(p: T) {
        s += "A"
        p.toString()
    }
}

class B : A<Function0<String>>() {
}


// FILE: 2.kt

fun box() : String {
    A<() -> String>().test { "123" }
    B().test { "123" }
    if (s != "AA") return "FAIL: $s"
    return "OK"
}
