interface A {
    fun <E> foo(a: E)
}

interface B {
    fun <T> foo(b: T)
}

interface C : A, B { // Warning here, this is correct
}

fun test(c: C) {
    c.foo(a = 1)
    c.<!INAPPLICABLE_CANDIDATE!>foo<!>(b = 1)
}