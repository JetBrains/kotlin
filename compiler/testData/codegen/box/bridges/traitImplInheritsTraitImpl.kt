interface A {
    fun foo(): Any = "A"
}

interface B : A {
    override fun foo(): String = "B"
}

class C : B

fun box(): String {
    val c = C()
    var r = c.foo() + (c : B).foo() + (c : A).foo()
    return if (r == "BBB") "OK" else "Fail: $r"
}
