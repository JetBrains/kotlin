open class A {
    open fun foo(): Any = "A"
}

trait B : A {
    override fun foo(): String = "B"
}

class C : A(), B

fun box(): String {
    val c = C()
    val r = c.foo() + (c : B).foo() + (c : A).foo()
    return if (r == "BBB") "OK" else "Fail: $r"
}
