open class A {
    open fun foo(): Any = "A"
}

trait B : A

open class C : A() {
    override fun foo(): Int = 222
}

trait D {
    fun foo(): Number
}

class E : B, C(), D

fun box(): String {
    val e = E()
    if (e.foo() != 222) return "Fail 1"
    if ((e : D).foo() != 222) return "Fail 2"
    if ((e : C).foo() != 222) return "Fail 3"
    if ((e : B).foo() != 222) return "Fail 4"
    if ((e : A).foo() != 222) return "Fail 5"
    return "OK"
}
