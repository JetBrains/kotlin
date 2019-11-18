// IGNORE_BACKEND_FIR: JVM_IR
open class A

class B : A() {
    fun foo(i: Int) = i
}

fun A.test() = if (this is B) foo(42) else 0

fun box(): String {
    if (B().test() != 42) return "fail1"
    if (A().test() != 0) return "fail2"
    return "OK"
}
