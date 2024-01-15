// FIR_IDENTICAL

interface A {
    val bar: String
}

interface B : A {
    override val bar: String
}

abstract class C(a: A) : B, A by a

fun foo(c: C) {
    c.bar
}