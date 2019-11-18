// IGNORE_BACKEND_FIR: JVM_IR
class A {
    inner class Inner {
        val o = 111
        val k = 222
    }
}

fun box(): String {
    val result = (A::Inner)((::A)()).o + (A::Inner)(A()).k
    if (result != 333) return "Fail $result"
    return "OK"
}
