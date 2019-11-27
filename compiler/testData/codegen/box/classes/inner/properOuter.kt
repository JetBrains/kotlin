// IGNORE_BACKEND_FIR: JVM_IR

open class A(val s: String) {
    open inner class B(val s: String) {
        fun testB() = s + this@A.s
    }

    open inner class C(): A("C") {
        fun testC() =
                B("B_").testB()
    }
}

fun box(): String {
    val res = A("A").C().testC()
    return if (res == "B_C") "OK" else res;
}