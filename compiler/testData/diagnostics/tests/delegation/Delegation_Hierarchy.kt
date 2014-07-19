trait A {
    fun foo() {}
}

open class B(a: A) : A by a

class C(a: A): B(a), A {
}

fun b(c: C) {
    c.foo();
}