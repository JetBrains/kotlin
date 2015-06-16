open class A {
    open fun foo(): Any = "A"
}

open class C : A() {
    override fun foo(): Int = 222
}

interface D {
    fun foo(): Number
}

class E : C(), D

fun box(): String {
    val e = E()
    if (e.foo() != 222) return "Fail 1"
    if ((e : D).foo() != 222) return "Fail 2"
    if ((e : C).foo() != 222) return "Fail 3"
    if ((e : A).foo() != 222) return "Fail 4"
    return "OK"
}
