// IGNORE_BACKEND_FIR: JVM_IR
// KT-4145

interface A {
    fun foo(): Any
}

open class B {
    fun foo(): String = "A"
}

open class C: B(), A

fun box(): String {
    val a: A = C()
    if (a.foo() != "A") return "Fail 1"
    if ((a as B).foo() != "A") return "Fail 2"
    if ((a as C).foo() != "A") return "Fail 3"
    return "OK"
}
