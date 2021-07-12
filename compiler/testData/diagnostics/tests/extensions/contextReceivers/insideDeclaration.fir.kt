class A {
    fun h1() {}
}

class B {
    fun h2() {}
}

fun B.foo() {
    <!UNRESOLVED_REFERENCE!>h1<!>()
    h2()
}

context(A)
fun B.bar() {
    <!UNRESOLVED_REFERENCE!>h1<!>()
    h2()
}
