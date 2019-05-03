interface A {
    fun foo(): Any
}

interface B {
    fun foo(): String = "A"
}

open class D: B

open class C: D(), A

fun box(): String {
    val a: A = C()
    if (a.foo() != "A") return "Fail 1"
    if ((a as B).foo() != "A") return "Fail 2"
    if ((a as C).foo() != "A") return "Fail 3"
    return "OK"
}
