// IGNORE_BACKEND_FIR: JVM_IR
open class A(val value: String) {
    fun component1() = value
}

interface B {
    fun component1(): Any
}

class C(value: String) : A(value), B

fun box(): String {
    val c = C("OK")
    val b: B = c
    val a: A = c
    if (b.component1() != "OK") return "Fail 1"
    if (a.component1() != "OK") return "Fail 2"
    return c.component1()
}
