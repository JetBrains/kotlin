abstract class A {
    abstract fun foo(): Any
}

interface B {
    fun foo(): String = "B"
}

interface C : A, B

class D : A(), C

fun box(): String {
    val d = D()
    val r = d.foo() + (d : C).foo() + (d : B).foo() + (d : A).foo()
    return if (r == "BBBB") "OK" else "Fail: $r"
}
