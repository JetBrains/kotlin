// IGNORE_BACKEND_FIR: JVM_IR
class A {
    fun foo(k: Int) = k

    fun result() = (A::foo)(this, 111)
}

fun box(): String {
    val result = A().result()
    if (result != 111) return "Fail $result"
    return "OK"
}
