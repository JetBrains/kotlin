// IGNORE_BACKEND_FIR: JVM_IR

open class A(val s: String) {

    val z = s

    fun test() = s

    open inner class B(s: String): A(s) {
        fun testB() = z + test()
    }
}

fun box(): String {
    val res = A("Fail").B("OK").testB()
    return if (res == "OKOK") "OK" else res;
}