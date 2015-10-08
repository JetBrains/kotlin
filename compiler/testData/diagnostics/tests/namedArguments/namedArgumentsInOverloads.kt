interface A {
    fun foo(a1: Int, a2: Double)
}

interface B {
    fun foo(b1: Int, b2: String)
}

interface C : A, B {}

fun test(d: C) {
    d.foo(a1 = 1, a2 = 1.0)
    d.foo(b1 = 1, b2 = "")
}