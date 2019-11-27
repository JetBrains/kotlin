// IGNORE_BACKEND_FIR: JVM_IR
class A {
    inner class Inner {
        val o = 111
        val k = 222
    }
}

fun A.foo() = (A::Inner)(this).o + (A::Inner)(this).k

fun box(): String {
    val result = A().foo()
    if (result != 333) return "Fail $result"
    return "OK"
}
